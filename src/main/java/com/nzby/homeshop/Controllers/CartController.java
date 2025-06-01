package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;
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

        model.addAttribute("cartItems", cartItems);

        BigDecimal totalWeightInKg = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            try {
                BigDecimal weightInKg = cartService.convertToKg(item.getProduct().getWeightValue(), item.getProduct().getWeightUnit());
                totalWeightInKg = totalWeightInKg.add(weightInKg.multiply(BigDecimal.valueOf(item.getQuantity())));
            } catch (IllegalArgumentException e) {
            }
        }

        BigDecimal maxWeightLimit = new BigDecimal("10.0"); // Лимит 10 кг
        boolean weightLimitExceeded = totalWeightInKg.compareTo(maxWeightLimit) > 0;

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalWeightInKg", totalWeightInKg);
        model.addAttribute("totalPrice", cartItems.stream().map(CartItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("cartCount", cartItems.stream().mapToInt(CartItem::getQuantity).sum());
        model.addAttribute("weightLimitExceeded", weightLimitExceeded);
        model.addAttribute("maxWeightLimit", maxWeightLimit);
        model.addAttribute("isAuthenticated", userDetails != null);

        return "cart";
    }

    @PostMapping("/update/{id}")
    public String updateQuantity(@PathVariable Long id,
                                 @RequestParam Integer quantityChange,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        try {
            CartItem cartItem = cartService.getCartItemById(id);
            if (cartItem != null && cartItem.getUser().getId().equals(user.getId())) {
                int newQuantity = cartItem.getQuantity() + quantityChange;

                if (newQuantity > MAX_QUANTITY) {
                    return "redirect:/cart";
                }

                cartService.updateCartItem(cartItem, quantityChange);
            }
        } catch (Exception e) {
            // Без сообщений об ошибке
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        try {
            CartItem cartItem = cartService.getCartItemById(id);
            if (cartItem != null && cartItem.getUser().getId().equals(user.getId())) {
                cartService.removeCartItem(cartItem);
            }
        } catch (Exception e) {
            // Без сообщений об ошибке
        }

        return "redirect:/cart";
    }
}