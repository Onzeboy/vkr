package com.nzby.homeshop.Controllers;


import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.ReviewService;
import com.nzby.homeshop.Services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    // Существующие методы для получения отзывов и создания отзыва остаются без изменений
//    @GetMapping("/product/{productId}")
//    public ResponseEntity<List<Review>> getReviewsByProductId(@PathVariable Long productId) {
//        try {
//            List<Review> reviews = reviewService.findByProductId(productId);
//            return ResponseEntity.ok(reviews);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long id) {
        try {
            Review review = reviewService.findById(id);
            if (review == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PostMapping
    @Transactional
    public ResponseEntity<String> createReview(
            @RequestParam Long productId,
            @RequestParam Integer rating,
            @RequestParam String comment,
            @RequestParam(required = false) Boolean isAnonymous,
            @RequestParam(required = false) List<MultipartFile> images) {
        try {
            // Валидация рейтинга
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body("Рейтинг должен быть от 1 до 5.");
            }

            // Валидация комментария
            if (comment == null || comment.trim().length() < 3) {
                return ResponseEntity.badRequest().body("Комментарий должен содержать минимум 3 символа.");
            }
            if (comment.trim().length() > 1000) {
                return ResponseEntity.badRequest().body("Комментарий не должен превышать 1000 символов.");
            }

            // Проверка аутентификации
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован.");
            }

            // Получение текущего пользователя
            User user = userService.getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Не удалось определить пользователя.");
            }

            // Проверка, существует ли уже отзыв от этого пользователя для данного продукта
            boolean hasExistingReview = reviewService.existsByProductIdAndUserId(productId, user.getId());
            if (hasExistingReview) {
                return ResponseEntity.badRequest().body("Вы уже оставили отзыв для этого продукта.");
            }

            // Создание отзыва
            Review review = reviewService.createReview(productId, rating, comment.trim(), isAnonymous, user, images);

            // Получение продукта и добавление отзыва
            Product product = productService.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Продукт с ID " + productId + " не найден"));
            product.addReview(review);

            // Сохранение продукта с обновленным рейтингом и количеством отзывов
            productService.saveProduct(product);

            return ResponseEntity.ok("Отзыв успешно добавлен!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при добавлении отзыва: " + e.getMessage());
        }
    }

    @PostMapping("/{reviewId}/vote")
    public ResponseEntity<Map<String, Object>> voteReview(@PathVariable Long reviewId, @RequestParam Integer vote) {
        try {
            // Validate authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Пользователь не авторизован."));
            }

            // Validate vote
            if (vote != 1 && vote != -1 && vote != 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Недопустимое значение голоса. Используйте +1, -1 или 0."));
            }

            User user = userService.getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Не удалось определить пользователя."));
            }

            Review review = reviewService.findById(reviewId);
            if (review == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Отзыв не найден."));
            }

            reviewService.voteReview(review, user, vote);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("voteScore", reviewService.getVoteScore(review));
            response.put("userVote", vote); // Return the vote just cast
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Ошибка при голосовании: " + e.getMessage()));
        }
    }
    @GetMapping("/load-more")
    public ResponseEntity<Map<String, Object>> loadMoreReviews(
            @RequestParam Long productId,
            @RequestParam int offset,
            @RequestParam int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("voteScore").descending());
        Page<Review> reviewPage = reviewService.findByProductIdWithPagination(productId, pageable);
        List<Review> reviews = reviewPage.getContent();

        Map<Long, Long> voteScores = new HashMap<>();
        Map<Long, Integer> userVote = new HashMap<>();
        User user = userService.getCurrentUser();
        for (Review review : reviews) {
            voteScores.put(review.getId(), reviewService.getVoteScore(review));
            if (user != null) {
                ReviewVote vote = reviewService.findUserVote(review, user);
                userVote.put(review.getId(), vote != null ? vote.getVote() : null);
            } else {
                userVote.put(review.getId(), null);
            }
        }

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> reviewData = reviews.stream().map(review -> {
            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("id", review.getId());
            reviewMap.put("isAnonymous", review.isAnonymous());
            reviewMap.put("user", review.getUser() != null ? Map.of("name", review.getUser().getName()) : null);
            reviewMap.put("createdAt", review.getCreatedAt().toString());
            reviewMap.put("rating", review.getRating());
            reviewMap.put("comment", review.getComment());
            reviewMap.put("images", review.getImages() != null ? review.getImages().stream()
                    .map(image -> Map.of("filePath", image.getFilePath()))
                    .collect(Collectors.toList()) : Collections.emptyList());
            reviewMap.put("voteScore", voteScores.get(review.getId()));
            reviewMap.put("userVote", userVote.get(review.getId()));
            return reviewMap;
        }).collect(Collectors.toList());

        response.put("reviews", reviewData);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getReviewsByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("voteScore").descending());
            Page<Review> reviewPage = reviewService.findByProductIdWithPagination(productId, pageable);
            List<Review> reviews = reviewPage.getContent();

            Map<Long, Long> voteScores = new HashMap<>();
            Map<Long, Integer> userVote = new HashMap<>();
            User user = userService.getCurrentUser();
            for (Review review : reviews) {
                voteScores.put(review.getId(), reviewService.getVoteScore(review));
                if (user != null) {
                    ReviewVote vote = reviewService.findUserVote(review, user);
                    userVote.put(review.getId(), vote != null ? vote.getVote() : null);
                } else {
                    userVote.put(review.getId(), null);
                }
            }

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> reviewData = reviews.stream().map(review -> {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("isAnonymous", review.isAnonymous());
                reviewMap.put("user", review.getUser() != null ? Map.of("name", review.getUser().getName()) : null);
                reviewMap.put("createdAt", review.getCreatedAt().toString());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("images", review.getImages() != null ? review.getImages().stream()
                        .map(image -> Map.of("filePath", image.getFilePath()))
                        .collect(Collectors.toList()) : Collections.emptyList());
                return reviewMap;
            }).collect(Collectors.toList());

            response.put("reviews", reviewData);
            response.put("userVote", userVote);
            response.put("voteScores", voteScores);
            response.put("totalPages", reviewPage.getTotalPages());
            response.put("totalElements", reviewPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка при загрузке отзывов: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}