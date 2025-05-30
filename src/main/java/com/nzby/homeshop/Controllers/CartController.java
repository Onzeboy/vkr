package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final ProductService productService;
    private final UserService userService;
    private static final int MAX_QUANTITY = 10;

    @Autowired
    public CartController(CartService cartService, ProductService productService, UserService userService) {
        this.cartService = cartService;
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/items")
    public ResponseEntity<?> getCartItems(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            List<CartItem> cartItems = cartService.getCartItems(user);
            return ResponseEntity.ok(cartItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long productId ,
                                       @RequestParam(defaultValue = "1") Integer quantity,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            Product product = productService.getProductById(productId);

            // Проверка максимального количества
            CartItem existingItem = cartService.findCartItemByUserAndProduct(user, product);
            if (existingItem != null && existingItem.getQuantity() + quantity > MAX_QUANTITY) {
                throw new IllegalStateException("Максимальное количество товара - " + MAX_QUANTITY + " шт.");
            }

            CartItem cartItem = cartService.addToCart(user, product, quantity);
            return ResponseEntity.ok(successResponse("Товар добавлен в корзину", cartItem));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long id,
                                            @RequestParam Integer quantityChange,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            CartItem cartItem = cartService.getCartItemById(id);

            if (!cartItem.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Недостаточно прав для изменения");
            }

            int newQuantity = cartItem.getQuantity() + quantityChange;

            // Проверка ограничений
            if (newQuantity > MAX_QUANTITY) {
                throw new IllegalStateException("Максимальное количество товара - " + MAX_QUANTITY + " шт.");
            }

            if (newQuantity < 1) {
                cartService.removeCartItem(cartItem);
                return ResponseEntity.ok(successResponse("Товар удален из корзины", null));
            }

            cartService.updateCartItem(cartItem, quantityChange);
            return ResponseEntity.ok(successResponse("Количество обновлено", cartItem));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            CartItem cartItem = cartService.getCartItemById(id);

            if (!cartItem.getUser().getId().equals(user.getId())) {
                throw new IllegalStateException("Недостаточно прав для удаления");
            }

            cartService.removeCartItem(cartItem);
            return ResponseEntity.ok(successResponse("Товар удален из корзины", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> successResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }

    private Map<String, Object> errorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        return response;
    }
}