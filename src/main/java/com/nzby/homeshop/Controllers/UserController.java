package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.DTO.UpdateProfileRequest;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.OrderService;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;


    @Autowired
    private CartService cartService;

    @GetMapping("/profile")
    public String getProfilePage(Model model) {

        model.addAttribute("user", userService.getCurrentUser());
        Order lastOrder = orderService.getLastOrderByUser(userService.getCurrentUser());
        model.addAttribute("lastOrder", lastOrder);
        List<Product> favorites = productService.get3WishlistProducts(userService.getCurrentUser());
        model.addAttribute("favorites", favorites);
        int cartCount = cartService.getCartItemsCount(userService.getCurrentUser());
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("isAuthenticated", true);
        return "user";
    }

    @PostMapping("/api/remove")
    public ResponseEntity<?> removeFromWishlist(@RequestBody Map<String, Long> request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Необходимо авторизоваться");
        }
        try {
            String email = authentication.getName();
            Long productId = request.get("productId");
            userService.removeFromWishlist(email, productId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/items")
    public ResponseEntity<?> getWishlistItems(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Необходимо авторизоваться");
        }
        try {
            String email = authentication.getName();
            List<Long> productIds = userService.getWishlistProductIds(email);
            return ResponseEntity.ok(productIds);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/api/add")
    public ResponseEntity<?> addToWishlist(@RequestBody Map<String, Long> request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Необходимо авторизоваться");
        }
        try {
            String email = authentication.getName();
            Long productId = request.get("productId");
            userService.addToWishlist(email, productId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/profile/update")
    public ResponseEntity<String> updateProfile(@Valid @RequestBody UpdateProfileRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            userService.updateProfile(request);
            return ResponseEntity.ok("Профиль успешно обновлен");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка при обновлении профиля");
        }
    }
}
