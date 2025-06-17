package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.POJO.CourierActivity;
import com.nzby.homeshop.POJO.Enum.ActivityType;
import com.nzby.homeshop.POJO.Enum.OrderStatus;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CourierActivityService;
import com.nzby.homeshop.Services.CourierService;
import com.nzby.homeshop.Services.OrderService;
import com.nzby.homeshop.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
public class OrderAdminController {

    @Autowired
    private  OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourierService courierService;

    @Autowired
    private CourierActivityService courierActivityService;
    @GetMapping
    public String showOrders(@RequestParam(required = false) OrderStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderService.getOrdersByStatus(status, pageable);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("page", orderPage);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("couriers", orderService.getAllCouriers());
        return "admin/orderslist";
    }
    @PostMapping("/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("message", "Статус заказа успешно обновлен!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка при обновлении статуса заказа: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
    @PostMapping("/couriers/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createCourier(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam String surname,
            @RequestParam(required = false) String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            User newCourier = courierService.createCourier(email, password, name, surname, phoneNumber);
            response.put("success", true);
            response.put("courier", newCourier);
            response.put("message", "Курьер успешно создан");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/couriers/{courierId}/activity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourierActivity(@PathVariable Long courierId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(courierId);
            if (user.getRole() != Role.COURIER) {
                throw new RuntimeException("Курьер не найден");
            }
            List<CourierActivity> activities = courierService.getCourierActivity(
                    user.getCourier(), LocalDateTime.now().minusDays(1));
            response.put("success", true);
            response.put("activities", activities);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/couriers/{courierId}/orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourierOrders(@PathVariable Long courierId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(courierId);
            if (user.getRole() != Role.COURIER) {
                throw new RuntimeException("Курьер не найден");
            }
            List<Order> orders = courierService.getCompletedOrders(user.getCourier());
            response.put("success", true);
            response.put("orders", orders);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
    @GetMapping("/assign-courier")
    public String showAssignCourierForm(@RequestParam Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("couriers", courierService.findOnlineCouriers());
        return "assign-courier";
    }

    @PostMapping("/assign-courier")
    public String assignCourier(@RequestParam Long orderId, @RequestParam(required = false) Long courierId, Model model) {
        try {
            orderService.assignCourierToOrder(orderId, courierId);
            model.addAttribute("message", "Курьер успешно назначен");
            courierActivityService.logActivity(courierService.findById(courierId), ActivityType.ORDER_ACCEPTED, "Выдан заказ");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("orderId", orderId);
            model.addAttribute("couriers", courierService.findOnlineCouriers());
            return "assign-courier";
        }
        return "redirect:/admin/orders";
    }
}