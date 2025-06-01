package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Repository.ProductRepository;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.ReviewService;
import com.nzby.homeshop.Services.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
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

    public ProductController(ProductService productService, UserService userService, ReviewService reviewService) {
        this.productService = productService;
        this.userService = userService;
        this.reviewService = reviewService;
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
}