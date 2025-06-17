package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.Address;
import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CartService;
import com.nzby.homeshop.Services.OrderService;
import com.nzby.homeshop.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final CartService cartService;

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    public OrderController(OrderService orderService, UserService userService,CartService cartService) {
        this.orderService = orderService;
        this.userService = userService;
        this.cartService = cartService;
    }

    @PostMapping("/checkout")
    public String proceedToCheckout(Model model, RedirectAttributes redirectAttributes) {
        logger.info("Переход на страницу оформления заказа");

        User user;
        try {
            user = userService.getCurrentUser();
        } catch (Exception e) {
            logger.error("Ошибка при получении текущего пользователя: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка авторизации");
            return "redirect:/cart";
        }

        // Получаем товары с isOrdered == true
        List<CartItem> orderedItems = cartService.getCartItemsByUserAndOrdered(user, true);
        if (orderedItems.isEmpty()) {
            logger.warn("Не выбрано ни одного товара для заказа (isOrdered == true)");
            redirectAttributes.addFlashAttribute("errorMessage", "Выберите хотя бы один товар для заказа");
            return "redirect:/cart";
        }

        // Проверяем аутентификацию
        boolean isAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);

        try {
            List<Address> userAddresses = orderService.getLastThreeUsedAddresses(user);
            model.addAttribute("userAddresses", userAddresses);
            model.addAttribute("cartCount", cartService.getCartItemsCount(user));
        } catch (Exception e) {
            logger.error("Ошибка при получении количества товаров в корзине: {}", e.getMessage());
            model.addAttribute("cartCount", 0);
        }

        return "checkout";
    }

    @PostMapping("/create")
    public String createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("selectedItems") String selectedItems,
            @RequestParam("streetAddress") String streetAddress,
            @RequestParam("city") String city,
            @RequestParam("paymentMethod") String paymentMethod,
            RedirectAttributes redirectAttributes) {

        logger.info("Начало создания заказа для пользователя: {}", userDetails.getUsername());

        User user;
        try {
            user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
            logger.debug("Пользователь найден: {}", user.getEmail());
        } catch (IllegalStateException e) {
            logger.error("Ошибка при поиске пользователя: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь не найден");
            return "redirect:/cart";
        }

        try {
            List<Long> selectedItemIds = Arrays.stream(selectedItems.split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            logger.debug("Выбранные товары: {}", selectedItemIds);

            Address address = new Address();
            address.setStreetAddress(streetAddress);
            address.setCity(city);

            Order order = orderService.createOrder(user, selectedItemIds, address, paymentMethod);
            logger.info("Заказ успешно создан: #{}", order.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Заказ #" + order.getId() + " успешно оформлен!");
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка преобразования данных: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка в данных заказа: " + e.getMessage());
            return "redirect:/cart";
        } catch (IllegalStateException e) {
            logger.warn("Ошибка при создании заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        } catch (Exception e) {
            logger.error("Необработанная ошибка при создании заказа: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при оформлении заказа");
            return "redirect:/cart";
        }

        return "redirect:/orders/success";
    }

    @GetMapping("/success")
    public String orderSuccess(Model model) {
        model.addAttribute("message", "Ваш заказ успешно оформлен!");
        return "order-success";
    }

    @GetMapping
    public String getOrders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = userService.getCurrentUser();

        // Получаем все заказы пользователя с изображениями
        List<Order> allOrders = orderService.getOrdersWithImages(user);

        // Разделяем заказы на активные и завершённые
        List<Order> activeOrders = allOrders.stream()
                .filter(order -> !order.getStatus().equals("COMPLETED"))
                .collect(Collectors.toList());

        List<Order> lastCompletedOrders = allOrders.stream()
                .filter(order -> order.getStatus().equals("COMPLETED"))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())) // Сортировка по дате (новые сверху)
                .limit(3) // Ограничиваем до 3
                .collect(Collectors.toList());

        // Добавляем в модель
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("lastCompletedOrders", lastCompletedOrders);
        model.addAttribute("cartCount", cartService.getCartItemsCount(user));

        return "orders";
    }
    @GetMapping("/{id}")
    public String getOrderDetails(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        Order order = orderService.getOrderWithDetails(id, user)
                .orElseThrow(() -> new IllegalStateException("Заказ не найден"));

        model.addAttribute("order", order);
        model.addAttribute("cartCount", cartService.getCartItemsCount(user));

        return "order-details";
    }
}