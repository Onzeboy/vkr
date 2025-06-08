package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.Services.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class UserAdminController {
    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public String showUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            Model model) {
        try {
            Role roleEnum = role != null && !role.isEmpty() ? Role.valueOf(role) : null;
            Page<User> userPage = userService.findUsers(email, roleEnum, PageRequest.of(page, size));
            model.addAttribute("users", userPage.getContent());
            model.addAttribute("currentPage", userPage.getNumber());
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalElements", userPage.getTotalElements());
            model.addAttribute("emailFilter", email != null ? email : "");
            model.addAttribute("roleFilter", role != null ? role : "");
            model.addAttribute("roles", Role.values());
            log.info("Загружен список пользователей: page={}, size={}, email={}, role={}", page, size, email, role);
            return "admin/users";
        } catch (IllegalArgumentException e) {
            log.error("Неверная роль: {}", role);
            model.addAttribute("error", "Неверная роль");
            return "admin/users";
        } catch (Exception e) {
            log.error("Ошибка при загрузке пользователей: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке пользователей");
            return "admin/users";
        }
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id, @RequestParam String role, Model model) {
        try {
            Role roleEnum = Role.valueOf(role);
            userService.updateUserRole(id, roleEnum);
            log.info("Роль пользователя обновлена: id={}, role={}", id, role);
            model.addAttribute("message", "Роль обновлена");
        } catch (IllegalArgumentException e) {
            log.error("Неверная роль для id {}: {}", id, e.getMessage());
            model.addAttribute("error", "Неверная роль");
        } catch (Exception e) {
            log.error("Ошибка при обновлении роли пользователя id {}: {}", id, e.getMessage());
            model.addAttribute("error", "Ошибка при обновлении роли");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/enabled")
    public String toggleUserEnabled(@PathVariable Long id, @RequestParam boolean enabled, Model model) {
        try {
            userService.toggleUserEnabled(id, enabled);
            log.info("Статус пользователя обновлен: id={}, enabled={}", id, enabled);
            model.addAttribute("message", "Статус обновлен");
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса пользователя id {}: {}", id, e.getMessage());
            model.addAttribute("error", "Ошибка при обновлении статуса");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Model model) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                log.error("Пользователь не найден для id: {}", id);
                model.addAttribute("error", "Пользователь не найден");
            } else {
                userService.deleteUser(id);
                log.info("Пользователь удалён: id={}", id);
                model.addAttribute("message", "Пользователь удалён");
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя id {}: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при удалении пользователя");
        }
        return "redirect:/admin/users";
    }
}