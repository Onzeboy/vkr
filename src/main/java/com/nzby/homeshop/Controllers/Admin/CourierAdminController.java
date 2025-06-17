package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.POJO.CourierActivity;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.Services.CourierService;
import com.nzby.homeshop.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class CourierAdminController {

    @Autowired
    private  UserService userService;

    @Autowired
    private CourierService courierService;

    @GetMapping("/couriers")
    public String getCouriersPage(Model model) {
        List<User> couriers = userService.findUsersByRole(Role.COURIER);
        model.addAttribute("couriers", couriers);
        return "admin/courierlist";
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
    @GetMapping("/couriers/generate-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generatePassword() {
        Map<String, Object> response = new HashMap<>();
        try {
            String password = courierService.generatePassword();
            response.put("success", true);
            response.put("password", password);
            response.put("message", "Пароль успешно сгенерирован");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при генерации пароля");
        }
        return ResponseEntity.ok(response);
    }
}