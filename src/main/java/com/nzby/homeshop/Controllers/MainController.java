package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.UserRepository;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String mainPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("mainPage вызван для пользователя: {}", userDetails != null ? userDetails.getUsername() : "неаутентифицирован");

        User user = null;
        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElse(null);
            if (user == null) {
                logger.warn("Пользователь с email {} не найден в базе данных", userDetails.getUsername());
            }
        }

        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("cartCount", user != null ? cartService.getCartItemsCount(user) : 0);
        model.addAttribute("isAuthenticated", userDetails != null);
        return "mainpage";
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("addToCart вызван с request: {}, user: {}",
                request, userDetails != null ? userDetails.getUsername() : "null");

        if (userDetails == null) {
            logger.error("Попытка добавить товар в корзину без аутентификации");
            return ResponseEntity.status(401).body("Пользователь не аутентифицирован");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь с email " + userDetails.getUsername() + " не найден"));

        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = request.containsKey("quantity") ? Integer.parseInt(request.get("quantity").toString()) : 1;

        Product product = productService.getProductById(productId);
        if (product == null) {
            logger.warn("Товар не найден для productId: {}", productId);
            return ResponseEntity.badRequest().body("Товар не найден");
        }

        try {
            CartItem cartItem = cartService.addToCart(user, product, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("cartItemId", cartItem.getId());
            response.put("totalItems", cartService.getCartItemsCount(user));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при добавлении товара: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при добавлении товара: {}", e.getMessage());
            return ResponseEntity.status(500).body("Произошла ошибка на сервере");
        }
    }

    @PostMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(@RequestBody Map<String, Object> request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.error("Попытка обновить корзину без аутентификации");
            return ResponseEntity.status(401).body("Пользователь не аутентифицирован");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь с email " + userDetails.getUsername() + " не найден"));

        Long cartItemId = Long.valueOf(request.get("cartItemId").toString());
        Integer quantity = Integer.parseInt(request.get("quantity").toString());

        CartItem cartItem = cartService.getCartItemById(cartItemId);
        if (cartItem == null || !cartItem.getUser().getId().equals(user.getId())) {
            logger.warn("Товар в корзине не найден или не принадлежит пользователю, cartItemId: {}", cartItemId);
            return ResponseEntity.badRequest().body("Товар в корзине не найден");
        }

        try {
            cartService.updateCartItem(cartItem, quantity - cartItem.getQuantity());
            Map<String, Object> response = new HashMap<>();
            response.put("totalItems", cartService.getCartItemsCount(user));
            response.put("quantity", cartItem.getQuantity());
            response.put("totalPrice", cartItem.getTotalPrice());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Ошибка при обновлении количества: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при обновлении количества: {}", e.getMessage());
            return ResponseEntity.status(500).body("Произошла ошибка на сервере");
        }
    }

    @DeleteMapping("/api/cart/remove")
    @ResponseBody
    public ResponseEntity<?> removeFromCart(@RequestParam Long cartItemId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.error("Попытка удалить товар из корзины без аутентификации");
            return ResponseEntity.status(401).body("Пользователь не аутентифицирован");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь с email " + userDetails.getUsername() + " не найден"));

        CartItem cartItem = cartService.getCartItemById(cartItemId);
        if (cartItem == null || !cartItem.getUser().getId().equals(user.getId())) {
            logger.warn("Товар в корзине не найден или не принадлежит пользователю, cartItemId: {}", cartItemId);
            return ResponseEntity.badRequest().body("Товар в корзине не найден");
        }

        try {
            cartService.removeFromCart(cartItemId);
            Map<String, Object> response = new HashMap<>();
            response.put("totalItems", cartService.getCartItemsCount(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при удалении товара: {}", e.getMessage());
            return ResponseEntity.status(500).body("Произошла ошибка на сервере");
        }
    }

    @GetMapping("/api/cart/items")
    @ResponseBody
    public ResponseEntity<?> getCartItems(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Пользователь не аутентифицирован");
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
        List<CartItem> cartItems = cartService.getCartItems(user);
        List<Map<String, Object>> items = cartItems.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("productId", item.getProduct().getId());
            map.put("quantity", item.getQuantity());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }
}