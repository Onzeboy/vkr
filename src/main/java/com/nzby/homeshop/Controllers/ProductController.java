package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Repository.ProductRepository;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.ReviewService;
import com.nzby.homeshop.Services.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final CartService cartService;

    public ProductController(ProductService productService, UserService userService, ReviewService reviewService, CartService cartService) {
        this.productService = productService;
        this.userService = userService;
        this.reviewService = reviewService;
        this.cartService = cartService;
    }

    @GetMapping("/{id}")
    @Transactional
    public String showProduct(@PathVariable Long id, Model model) {
        // Загружаем продукт
        Product product = productService.findProductById(id);
        if (product == null) {
            model.addAttribute("errorMessage", "Продукт не найден");
            return "product-details";
        }

        // Получаем отсортированные изображения
        List<ProductImage> sortedImages = productService.getSortedImagesForProduct(id);

        // Синхронизируем product.images с sortedImages
        if (product.getImages() != null) {
            product.getImages().clear();
            product.getImages().addAll(sortedImages);
        } else {
            // Если коллекция не инициализирована, создаём новый список
            product.setImages(sortedImages);
        }

        // Выбираем основное изображение
        ProductImage primaryImage = productService.getPrimaryImage(sortedImages);

        // Загружаем и сортируем отзывы по voteScore
        List<Review> reviews = reviewService.findByProductId(id);
        if (reviews != null && !reviews.isEmpty()) {
            reviews.sort((r1, r2) -> {
                Long score1 = reviewService.getVoteScore(r1);
                Long score2 = reviewService.getVoteScore(r2);
                return score2.compareTo(score1); // Сортировка по убыванию
            });
            reviews = reviews.stream().limit(3).collect(Collectors.toList());
        }

        // Рассчитываем voteScore для каждого отзыва
        Map<Long, Long> voteScores = new HashMap<>();
        Map<Long, Integer> userVote = new HashMap<>();
        for (Review review : reviews) {
            voteScores.put(review.getId(), reviewService.getVoteScore(review));
            User user = userService.getCurrentUser();
            if (user != null) {
                ReviewVote vote = reviewService.findUserVote(review, user);
                userVote.put(review.getId(), vote != null ? vote.getVote() : null);
            } else {
                userVote.put(review.getId(), null);
            }
        }

        model.addAttribute("product", product);
        model.addAttribute("primaryImage", primaryImage);
        model.addAttribute("categoryFields", product.getCategory() != null ? product.getCategory().getProductFields() : new HashMap<>());
        model.addAttribute("reviews", reviews);
        model.addAttribute("voteScores", voteScores);
        model.addAttribute("userVote", userVote);

        return "product-details";
    }
    @GetMapping("/reviews/{id}")
    @Transactional
    public String showAllReviews(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, Model model) {
        Product product = productService.findProductById(id);
        if (product == null) {
            model.addAttribute("errorMessage", "Продукт не найден");
            return "all-reviews";
        }

        // Получаем отсортированные изображения
        List<ProductImage> sortedImages = productService.getSortedImagesForProduct(id);
        if (product.getImages() != null) {
            product.getImages().clear();
            product.getImages().addAll(sortedImages);
        } else {
            product.setImages(sortedImages);
        }
        ProductImage primaryImage = productService.getPrimaryImage(sortedImages);

        // Получаем отзывы с пагинацией и сортировкой по voteScore
        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by("voteScore").descending());
        Page<Review> reviewPage = reviewService.findByProductIdWithPagination(id, pageable);
        List<Review> reviews = reviewPage.getContent();

        // Рассчитываем voteScore и userVote
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

        model.addAttribute("product", product);
        model.addAttribute("primaryImage", primaryImage);
        model.addAttribute("categoryFields", product.getCategory() != null ? product.getCategory().getProductFields() : new HashMap<>());
        model.addAttribute("reviews", reviews);
        model.addAttribute("voteScores", voteScores);
        model.addAttribute("userVote", userVote);
        model.addAttribute("totalReviews", product.getRatingsCount() != null ? product.getRatingsCount() : 0);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewPage.getTotalPages());
        model.addAttribute("cartCount", cartService.getCartItemsCount(userService.getCurrentUser()));

        return "all-reviews";
    }

}