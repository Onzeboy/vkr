package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.Review;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.ReviewService;
import com.nzby.homeshop.Services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    // Существующие методы для получения отзывов и создания отзыва остаются без изменений
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProductId(@PathVariable Long productId) {
        try {
            List<Review> reviews = reviewService.findByProductId(productId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

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
}