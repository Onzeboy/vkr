package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Repository.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ReviewVoteRepository reviewVoteRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final String UPLOAD_DIR = "uploaded-review-images/";

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, ProductService productService, ReviewImageRepository reviewImageRepository, UserRepository userRepository, ReviewVoteRepository reviewVoteRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.userRepository = userRepository;
        this.reviewVoteRepository = reviewVoteRepository;


        // Ensure upload directory exists
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }


    public List<ReviewImage> processAndSaveReviewPhotos(Review review, List<MultipartFile> images) throws IllegalArgumentException {
        List<ReviewImage> reviewImages = new ArrayList<>();
        logger.info("Processing {} photos for review ID: {}", images != null ? images.size() : 0, review.getId());

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Необходимо загрузить хотя бы одно изображение");
        }

        if (images.size() > 10) {
            throw new IllegalArgumentException("Максимум 10 фотографий");
        }

        // Разрешенные расширения файлов
        List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png");

        // Директория для отзывов
        String reviewDir = Paths.get(uploadDir, "reviews", String.valueOf(review.getId())).toString();
        Path dirPath = Paths.get(reviewDir);

        try {
            Files.createDirectories(dirPath);
            logger.info("Directory created successfully: {}", reviewDir);
        } catch (IOException e) {
            logger.error("Failed to create directory: {}, error: {}", reviewDir, e.getMessage());
            throw new IllegalArgumentException("Не удалось создать папку для изображений: " + e.getMessage());
        }

        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                String contentType = image.getContentType();
                String originalFilename = image.getOriginalFilename();
                logger.info("Processing photo: name={}, size={}, type={}", originalFilename, image.getSize(), contentType);

                // Проверка расширения файла
                String fileExtension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                        : "";
                if (!allowedExtensions.contains(fileExtension)) {
                    logger.warn("Invalid file extension for {}: {}", originalFilename, fileExtension);
                    throw new IllegalArgumentException("Файл " + originalFilename + ": разрешены только файлы с расширениями .jpg, .jpeg, .png");
                }

                if (contentType == null || !Arrays.asList("image/jpeg", "image/png").contains(contentType)) {
                    logger.warn("Invalid file format for {}: {}", originalFilename, contentType);
                    throw new IllegalArgumentException("Файл " + originalFilename + ": разрешены только JPEG и PNG");
                }

                if (image.getSize() > 5 * 1024 * 1024) {
                    logger.warn("File {} exceeds 5MB limit", originalFilename);
                    throw new IllegalArgumentException("File " + originalFilename + ": размер не должен превышать 5 МБ");
                }

                try {
                    // Генерируем уникальное имя файла
                    String fileName = UUID.randomUUID().toString() + fileExtension;
                    Path filePath = dirPath.resolve(fileName);
                    Files.write(filePath, image.getBytes());
                    logger.info("Saved photo for review {}: {}", review.getId(), filePath);

                    // Сохраняем относительный путь
                    String relativeFilePath = "reviews/" + review.getId() + "/" + fileName;

                    ReviewImage reviewImage = new ReviewImage();
                    reviewImage.setReview(review);
                    reviewImage.setFilePath(relativeFilePath);
                    reviewImages.add(reviewImage);

                    reviewImageRepository.save(reviewImage);
                } catch (IOException e) {
                    logger.error("Failed to save photo {} for review {}: {}", originalFilename, review.getId(), e.getMessage());
                    throw new IllegalArgumentException("Ошибка при загрузке файла " + originalFilename + ": " + e.getMessage());
                }
            }
        }

        return reviewImages;
    }

    public Review createReview(Long productId, Integer rating, String comment, Boolean isAnonymous,
                               User user, List<MultipartFile> images) {
        // Проверка существования продукта
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Продукт с ID " + productId + " не найден"));

        // Проверка, что пользователь не null
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }

        // Проверка существования отзыва от этого пользователя для данного продукта
        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new IllegalArgumentException("Вы уже оставили отзыв для этого продукта.");
        }

        // Создание нового отзыва
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);
        review.setAnonymous(isAnonymous != null ? isAnonymous : false);
        review.setCreatedAt(LocalDateTime.now());

        // Сохранение отзыва
        Review savedReview = save(review);

        // Обработка и сохранение фотографий, если они предоставлены
        if (images != null && !images.isEmpty()) {
            List<ReviewImage> reviewImages = processAndSaveReviewPhotos(savedReview, images);
            savedReview.setImages(reviewImages); // Устанавливаем изображения в отзыв
            save(savedReview); // Сохраняем отзыв с обновленными изображениями
        }

        return savedReview;
    }

    public boolean existsByProductIdAndUserId(Long productId, Long userId) {
        return reviewRepository.existsByProductIdAndUserId(productId, userId);
    }

    // Retrieve reviews by product ID
    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    // Find reviews by product ID (alias for getReviewsByProductId)
    public List<Review> findByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    // Find review by ID
    public Review findById(Long id) {
        Optional<Review> review = reviewRepository.findById(id);
        return review.orElseThrow(() -> new IllegalArgumentException("Отзыв с ID " + id + " не найден"));
    }

    // Save a review
    public Review save(@Valid Review review) {
        if (review.getCreatedAt() == null) {
            review.setCreatedAt(LocalDateTime.now());
        }
        return reviewRepository.save(review);
    }

    public List<String> saveReviewImages(List<MultipartFile> images) {
        List<String> filePaths = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return filePaths;
        }

        try {
            for (MultipartFile file : images) {
                if (!file.isEmpty() && file.getContentType() != null && file.getContentType().startsWith("image/")) {
                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    Path filePath = Paths.get(UPLOAD_DIR, fileName);
                    Files.write(filePath, file.getBytes());
                    String relativePath = "review-images/" + fileName;
                    filePaths.add(relativePath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении изображений: " + e.getMessage());
        }

        return filePaths;
    }

    // Delete review images
    public void deleteReviewImages(List<ReviewImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (ReviewImage image : images) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR, image.getFilePath().replace("review-images/", ""));
                Files.deleteIfExists(filePath);
                reviewImageRepository.delete(image);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при удалении изображения: " + e.getMessage());
            }
        }
    }

    // Delete a review and its images
    public void delete(Review review) {
        if (review != null) {
            deleteReviewImages(review.getImages());
            reviewRepository.delete(review);
        }
    }

    public Long getVoteScore(Review review) {
        return reviewVoteRepository.sumVotesByReview(review);
    }
    public ReviewVote findUserVote(Review review, User user) {
        return reviewVoteRepository.findByReviewAndUser(review, user);
    }

    @Transactional
    public void voteReview(Review review, User user, Integer vote) {
        if (vote != 1 && vote != -1 && vote != 0) {
            throw new IllegalArgumentException("Недопустимое значение голоса. Используйте +1, -1 или 0.");
        }

        ReviewVote existingVote = reviewVoteRepository.findByReviewAndUser(review, user);

        if (vote == 0) {
            if (existingVote != null) {
                reviewVoteRepository.delete(existingVote);
            }
        } else {
            if (existingVote != null) {
                if (existingVote.getVote() == vote) {
                    // Cancel vote
                    reviewVoteRepository.delete(existingVote);
                } else {
                    // Change vote
                    existingVote.setVote(vote);
                    reviewVoteRepository.save(existingVote);
                }
            } else {
                // New vote
                ReviewVote newVote = new ReviewVote(review, user, vote);
                reviewVoteRepository.save(newVote);
            }
        }
    }
}