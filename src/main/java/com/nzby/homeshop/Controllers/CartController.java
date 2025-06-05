package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private static final int MAX_QUANTITY = 10;
    @Autowired
    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping
    public String viewCart(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        List<CartItem> cartItems = cartService.getCartItems(user);
        cartItems.sort(Comparator.comparing(CartItem::getId));

        BigDecimal totalWeightInKg = cartService.calculateSelectedTotalWeight(cartItems,
                cartItems.stream().filter(CartItem::getOrdered).map(CartItem::getId).collect(Collectors.toList()));
        BigDecimal totalPrice = cartService.calculateSelectedTotalPrice(cartItems,
                cartItems.stream().filter(CartItem::getOrdered).map(CartItem::getId).collect(Collectors.toList()));

        BigDecimal maxWeightLimit = new BigDecimal("10.0");
        boolean weightLimitExceeded = totalWeightInKg.compareTo(maxWeightLimit) > 0;

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalWeightInKg", totalWeightInKg);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartCount", cartItems.stream().mapToInt(CartItem::getQuantity).sum());
        model.addAttribute("weightLimitExceeded", weightLimitExceeded);
        model.addAttribute("maxWeightLimit", maxWeightLimit);
        model.addAttribute("isAuthenticated", userDetails != null);

        return "cart";
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantityAjax(
            @PathVariable Long id,
            @RequestParam Integer quantityChange,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
            CartItem cartItem = cartService.getCartItemById(id);

            if (cartItem == null || !cartItem.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Элемент корзины не найден или не принадлежит пользователю");
                return ResponseEntity.badRequest().body(response);
            }

            int currentQuantity = cartItem.getQuantity();
            int newQuantity = currentQuantity + quantityChange;
            int stockQuantity = cartService.getProductStock(cartItem.getProduct().getId());
            int maxAllowed = Math.min(stockQuantity, MAX_QUANTITY);

            if (newQuantity <= 0) {
                cartService.removeCartItem(cartItem);
                response.put("success", true);
                response.put("remove", true);
                return ResponseEntity.ok(response);
            }

            if (newQuantity > maxAllowed) {
                response.put("success", false);
                response.put("message", "Максимальное количество для этого товара: " + maxAllowed + " шт.");
                return ResponseEntity.badRequest().body(response);
            }

            cartService.updateCartItem(cartItem, quantityChange);
            response.put("success", true);
            response.put("newQuantity", newQuantity);
            response.put("totalPrice", cartItem.getTotalPrice().doubleValue());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Ошибка при обновлении количества: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Необработанная ошибка: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Произошла ошибка при обновлении количества");
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCartAjax(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
            CartItem cartItem = cartService.getCartItemById(id);

            if (cartItem == null || !cartItem.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Элемент корзины не найден или не принадлежит пользователю");
                return ResponseEntity.badRequest().body(response);
            }

            cartService.removeCartItem(cartItem);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Ошибка при удалении: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Необработанная ошибка: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Произошла ошибка при удалении товара");
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam("selectedItems") String selectedItems,
                           @RequestParam("shippingAddress") String shippingAddress,
                           @RequestParam("shippingMethod") String shippingMethod,
                           @RequestParam("paymentMethod") String paymentMethod,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        try {
            List<Long> selectedItemIds = Arrays.stream(selectedItems.split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Выберите хотя бы один товар для заказа");
                return "redirect:/cart";
            }
            cartService.checkout(user, selectedItemIds); // This will be replaced by OrderService
            redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно оформлен!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }

        return "redirect:/orders/success";
    }

    @PostMapping("/update-selection/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSelection(
            @PathVariable Long id,
            @RequestParam Boolean isSelected,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
            CartItem cartItem = cartService.getCartItemById(id);

            if (cartItem == null || !cartItem.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Элемент корзины не найден или не принадлежит пользователю");
                return ResponseEntity.badRequest().body(response);
            }

            cartService.updateCartItemSelection(cartItem, isSelected);
            response.put("success", true);
            response.put("ordered", cartItem.getOrdered()); // Возвращаем текущее состояние
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Ошибка при обновлении выбора: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Необработанная ошибка: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Произошла ошибка при обновлении выбора");
            return ResponseEntity.status(500).body(response);
        }
    }
}