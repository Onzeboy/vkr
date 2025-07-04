package com.nzby.homeshop.Services;

import com.nzby.homeshop.DTO.CategoryForm;
import com.nzby.homeshop.POJO.Category;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.ProductImage;
import com.nzby.homeshop.POJO.Review;
import com.nzby.homeshop.Repository.CategoryRepository;
import com.nzby.homeshop.Repository.ProductImageRepository;
import com.nzby.homeshop.Repository.ProductRepository;
import com.nzby.homeshop.Repository.ReviewRepository;
import com.nzby.homeshop.Utils.ProductImageComparator;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;


    private static final int PAGE_SIZE = 9;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        logger.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        logger.info("Retrieved {} products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String search) {
        logger.info("Searching products with query: {}", search);
        List<Product> products = productRepository.findByNameContainingIgnoreCase(search);
        logger.info("Found {} products matching query", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public Product findProductById(Long id) {
        logger.info("Fetching product with ID: {}", id);
        Product product = productRepository.findByIdWithCategoryAndImages(id);
        if (product == null) {
            throw new IllegalArgumentException("Product with ID " + id + " not found");
        }
        logger.info("Product found: {}, Images: {}", product.getName(), product.getImages().size());
        return product;
    }

    @Cacheable(value = "rootCategories")
    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        logger.info("Fetching root categories");
        return categoryRepository.findByParentIsNull();
    }

    @Transactional(readOnly = true)
    public Category findCategoryById(Long id) {
        logger.info("Fetching category with ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category with ID " + id + " not found"));
    }

    @Cacheable(value = "subcategories", key = "#parentId")
    @Transactional(readOnly = true)
    public List<CategoryForm> getSubcategories(Long parentId) {
        logger.info("Fetching subcategories for parent ID: {}", parentId);
        if (!categoryRepository.existsById(parentId)) {
            logger.warn("Category with ID {} not found", parentId);
            return Collections.emptyList();
        }
        List<Category> subcategories = categoryRepository.findByParentCategoryId(parentId);
        List<CategoryForm> result = subcategories.stream()
                .map(category -> new CategoryForm(category.getId(), category.getName(), category.getProductFields()))
                .collect(Collectors.toList());
        logger.info("Returning {} subcategories", result.size());
        return result;
    }


    @Transactional
    public List<ProductImage> processAndSavePhotos(Product product, MultipartFile[] photos, BindingResult result, String primaryImage) {
        List<ProductImage> productImages = new ArrayList<>();
        logger.info("Processing {} photos for product ID: {}", photos != null ? photos.length : 0, product.getId());

        if (photos == null || photos.length == 0 || (photos.length == 1 && photos[0].isEmpty())) {
            result.rejectValue("images", "images.empty", "Необходимо загрузить хотя бы одно изображение");
            return productImages;
        }

        if (photos.length > 10) {
            result.rejectValue("images", "images.limit", "Максимум 10 фотографий");
            return productImages;
        }

        // Разрешенные расширения файлов
        List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png");

        // Используем app.upload.dir из конфигурации
        String productDir = Paths.get(uploadDir, "products", String.valueOf(product.getId())).toString();
        Path dirPath = Paths.get(productDir);

        try {
            Files.createDirectories(dirPath);
            logger.info("Directory created successfully: {}", productDir);
        } catch (IOException e) {
            logger.error("Failed to create directory: {}, error: {}", productDir, e.getMessage());
            result.rejectValue("images", "images.directory.error", "Не удалось создать папку для изображений");
            return productImages;
        }

        for (int i = 0; i < photos.length; i++) {
            MultipartFile photo = photos[i];
            if (photo != null && !photo.isEmpty()) {
                String contentType = photo.getContentType();
                String originalFilename = photo.getOriginalFilename();
                logger.info("Processing photo {}: name={}, size={}, type={}", i, originalFilename, photo.getSize(), contentType);

                // Проверка расширения файла
                String fileExtension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                        : "";
                if (!allowedExtensions.contains(fileExtension)) {
                    logger.warn("Invalid file extension for {}: {}", originalFilename, fileExtension);
                    result.rejectValue("images", "images.extension", "Файл " + originalFilename + ": разрешены только файлы с расширениями .jpg, .jpeg, .png");
                    continue;
                }

                if (contentType == null || !Arrays.asList("image/jpeg", "image/png").contains(contentType)) {
                    logger.warn("Invalid file format for {}: {}", originalFilename, contentType);
                    result.rejectValue("images", "images.format", "Файл " + originalFilename + ": разрешены только JPEG и PNG");
                    continue;
                }

                if (photo.getSize() > 5 * 1024 * 1024) {
                    logger.warn("File {} exceeds 5MB limit", originalFilename);
                    result.rejectValue("images", "images.size", "File " + originalFilename + ": размер не должен превышать 5 МБ");
                    continue;
                }

                try {
                    // Генерируем уникальное имя файла
                    String fileName = UUID.randomUUID().toString() + fileExtension;
                    Path filePath = dirPath.resolve(fileName);
                    Files.write(filePath, photo.getBytes());
                    logger.info("Saved photo for product {}: {}", product.getId(), filePath);

                    // Сохраняем относительный путь
                    String relativeFilePath = "products/" + product.getId() + "/" + fileName;

                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setFilePath(relativeFilePath);
                    productImage.setFileName(originalFilename);
                    productImage.setPosition(i);
                    productImage.setPrimary(primaryImage != null && primaryImage.equals("new_" + i));
                    productImages.add(productImage);

                    productImageRepository.save(productImage);
                } catch (IOException e) {
                    logger.error("Failed to save photo {} for product {}: {}", originalFilename, product.getId(), e.getMessage());
                    result.rejectValue("images", "images.upload.error", "Ошибка при загрузке файла " + originalFilename + ": " + e.getMessage());
                }
            }
        }

        return productImages;
    }

    @Transactional
    public void updateImagePositions(Product product) {
        logger.info("Updating image positions for product ID: {}", product.getId());
        List<ProductImage> images = product.getImages();
        int position = 0;
        for (ProductImage image : images) {
            image.setPosition(position++);
            productImageRepository.save(image);
        }
        logger.info("Updated {} image positions for product ID: {}", images.size(), product.getId());
    }

    @Transactional
    public void setPrimaryImage(Product product, Long primaryImageId) {
        logger.info("Setting primary image ID: {} for product ID: {}", primaryImageId, product.getId());
        List<ProductImage> images = product.getImages();
        boolean found = false;

        for (ProductImage image : images) {
            if (image.getId() != null && image.getId().equals(primaryImageId)) {
                image.setPrimary(true);
                found = true;
            } else {
                image.setPrimary(false);
            }
            productImageRepository.save(image);
        }

        if (!found && !images.isEmpty()) {
            images.get(0).setPrimary(true);
            productImageRepository.save(images.get(0));
            logger.info("Primary image ID {} not found, set first image as primary for product ID: {}", primaryImageId, product.getId());
        } else if (!found) {
            logger.warn("No images available to set as primary for product ID: {}", product.getId());
        }
    }

    @Transactional
    @CacheEvict(value = {"rootCategories", "subcategories"}, allEntries = true)
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Продукт с ID " + productId + " не найден"));
        logger.info("Удаление продукта: {}", product.getName());

        String productDir = Paths.get(uploadDir, "products", String.valueOf(productId)).toString();
        Path dirPath = Paths.get(productDir);
        try {
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                logger.info("Directory deleted: {}", productDir);
            }
        } catch (IOException e) {
            logger.warn("Не удалось удалить директорию: {}, error: {}", productDir, e.getMessage());
        }

        productRepository.delete(product);
    }

    @Transactional
    public void setupCategories(Product product, List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long categoryId : categoryIds) {
                Category category = findCategoryById(categoryId);
                categories.add(category);
            }
            product.setCategories(categories);
        }
    }

    @Transactional
    public void setupTags(Product product, String tags) {
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagSet = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
            product.setTags(tagSet);
        }
    }


    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        logger.info("Fetching all categories");
        return categoryRepository.findAll();
    }

    @Transactional
    public Product saveProduct(Product product) {
        logger.info("Saving product: {}", product.getName());
        Product savedProduct = productRepository.save(product);
        logger.info("Product saved with ID: {}", savedProduct.getId());
        // Явное обновление объекта после сохранения
        return productRepository.refreshWithCategoryAndImages(savedProduct.getId());
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStatus(ProductStatus productStatus) {
        logger.info("Fetching products by status: {}", productStatus);
        List<Product> products = productRepository.findByStatus(productStatus);
        products.forEach(this::setDefaultPhotoIfNull);
        logger.info("Retrieved {} products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        logger.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        products.forEach(this::setDefaultPhotoIfNull);
        logger.info("Retrieved {} products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> getRandomProducts(int limit) {
        logger.info("Fetching {} random products", limit);
        List<Product> products = productRepository.findRandomProducts(limit);
        products.forEach(this::setDefaultPhotoIfNull);
        logger.info("Retrieved {} random products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getSortedImagesForProduct(Long productId) {
        Product product = findProductById(productId);
        if (product == null || product.getImages() == null) {
            return new ArrayList<>(); // Возвращаем пустой список, если продукта или изображений нет
        }

        // Создаём новый список из изображений продукта
        List<ProductImage> images = new ArrayList<>(product.getImages());
        images.sort(ProductImageComparator.INSTANCE);
        return images;
    }

    @Transactional(readOnly = true)
    public ProductImage getPrimaryImage(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        // Возвращаем первое изображение с isPrimary = true или первое изображение, если основного нет
        return images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .orElse(images.get(0));
    }

    @Transactional
    public void updateProduct(Product product) {
        logger.info("Updating product: {}", product.getName());
        Product updatedProduct = productRepository.save(product);
        // Явное обновление после редактирования
        productRepository.refreshWithCategoryAndImages(updatedProduct.getId());
    }

    private void setDefaultPhotoIfNull(Product product) {
        // Логика по умолчанию не требуется, так как изображения управляются через ProductImage
    }

    @Transactional
    public void deleteImages(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            logger.info("No image IDs provided for deletion");
            return;
        }

        for (Long imageId : imageIds) {
            try {
                ProductImage image = productImageRepository.findById(imageId)
                        .orElseThrow(() -> new IllegalArgumentException("Image with ID " + imageId + " not found"));
                logger.info("Deleting image with ID: {}", imageId);

                String filePath = "src/main/resources/static/" + image.getFilePath();
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    logger.info("Deleted image file: {}", filePath);
                } else {
                    logger.warn("Image file not found: {}", filePath);
                }

                productImageRepository.deleteById(imageId);
                logger.info("Deleted image record with ID: {}", imageId);
            } catch (IOException e) {
                logger.error("Failed to delete image file for ID {}: {}", imageId, e.getMessage());
                throw new RuntimeException("Failed to delete image file with ID " + imageId, e);
            } catch (Exception e) {
                logger.error("Failed to delete image with ID {}: {}", imageId, e.getMessage());
                throw new RuntimeException("Failed to delete image with ID " + imageId, e);
            }
        }
    }


    public void deleteImageFiles(List<Long> imageIds) {
        List<ProductImage> images = productImageRepository.findAllById(imageIds);
        for (ProductImage image : images) {
            try {
                Path filePath = Paths.get(uploadDir, image.getFilePath());
                Files.deleteIfExists(filePath);
                logger.info("Deleted image file: {}", filePath);
            } catch (IOException e) {
                logger.error("Failed to delete image file: {}", image.getFilePath(), e);
            }
        }
    }
    public Optional<Product> findById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product;
    }

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }


    public Page<Product> findProducts(String search, String categoryId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Category category = null;
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            try {
                category = categoryRepository.findById(Long.valueOf(categoryId)).orElse(null);
            } catch (NumberFormatException e) {
                category = null;
            }
        }

        if (search != null && !search.trim().isEmpty() && category != null) {
            return productRepository.findByNameContainingIgnoreCaseAndCategory(search, category, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            return productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (category != null) {
            return productRepository.findByCategory(category, pageable);
        } else {
            return productRepository.findAll(pageable);
        }
    }

    public Page<Product> searchProducts(String search, Long categoryId, Pageable pageable) {
        String searchTerm = search != null && !search.isBlank() ? search.toLowerCase() : null;
        return productRepository.searchProducts(searchTerm, categoryId, pageable);
    }

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }
    public Map<Long, Integer> getStock() {
        return productRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product.getStock() != null ? product.getStock() : 0
                ));
    }
}