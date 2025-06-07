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
    public String showUsersPage(Model model) {
        return "admin/users";
    }

    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role) {
        try {
            Role roleEnum = role != null && !role.isEmpty() ? Role.valueOf(role) : null;
            Page<User> userPage = userService.findUsers(email, roleEnum, PageRequest.of(page, size));
            log.info("Загружен список пользователей: page={}, size={}, email={}, role={}", page, size, email, role);
            return ResponseEntity.ok(userPage);
        } catch (IllegalArgumentException e) {
            log.error("Неверная роль: {}", role);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверная роль"));
        } catch (Exception e) {
            log.error("Ошибка при загрузке пользователей: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при загрузке пользователей"));
        }
    }

    @GetMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                log.error("Пользователь не найден для id: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
            }
            log.info("Пользователь загружен: id={}", id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Ошибка при загрузке пользователя id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при загрузке пользователя"));
        }
    }

    @PutMapping("/api/users/{id}/role")
    @ResponseBody
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String roleStr = request.get("role");
            if (roleStr == null || roleStr.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Роль не указана"));
            }
            Role role = Role.valueOf(roleStr);
            userService.updateUserRole(id, role);
            log.info("Роль пользователя обновлена: id={}, role={}", id, role);
            return ResponseEntity.ok(Map.of("message", "Роль обновлена"));
        } catch (IllegalArgumentException e) {
            log.error("Неверная роль для id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверная роль"));
        } catch (Exception e) {
            log.error("Ошибка при обновлении роли пользователя id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при обновлении роли"));
        }
    }

    @PutMapping("/api/users/{id}/enabled")
    @ResponseBody
    public ResponseEntity<?> toggleUserEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Статус не указан"));
            }
            userService.toggleUserEnabled(id, enabled);
            log.info("Статус пользователя обновлен: id={}, enabled={}", id, enabled);
            return ResponseEntity.ok(Map.of("message", "Статус обновлен"));
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса пользователя id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при обновлении статуса"));
        }
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                log.error("Пользователь не найден для id: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
            }
            userService.deleteUser(id);
            log.info("Пользователь удалён: id={}", id);
            return ResponseEntity.ok(Map.of("message", "Пользователь удалён"));
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при удалении пользователя"));
        }
    }
}