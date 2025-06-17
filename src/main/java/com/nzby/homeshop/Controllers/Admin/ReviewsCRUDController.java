package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.Review;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ReviewsCRUDController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductService productService;


    @GetMapping("/admin/products/reviews/{id}")
    public String showAdminReviews (@PathVariable Long id, Model model) {

        Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        Page<Review> reviews = reviewService.getReviewsByProduct(id, PageRequest.of(0, 10));
        Map<Long, Integer> voteScores = new HashMap<>();
        Map<Long, Integer> userVotes = new HashMap<>();
        reviews.forEach(review -> {
            voteScores.put(review.getId(), reviewService.getVoteScoreAdmin(review.getId()));
            userVotes.put(review.getId(), 0); // Admin doesn't vote, so set to 0
        });

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("totalReviews", reviews.getTotalElements());
        model.addAttribute("voteScores", voteScores);
        model.addAttribute("userVote", userVotes);

        return "admin/adminreviews";
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    public ResponseEntity<String> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok("Review deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting review: " + e.getMessage());
        }
    }
}
