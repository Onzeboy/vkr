package com.nzby.homeshop.Controllers.Courier;

import com.nzby.homeshop.DTO.*;
import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.CourierActivity;
import com.nzby.homeshop.POJO.Enum.ActivityType;
import com.nzby.homeshop.POJO.Enum.AvailabilityStatus;
import com.nzby.homeshop.POJO.Enum.OrderStatus;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Services.CourierActivityService;
import com.nzby.homeshop.Services.CourierService;
import com.nzby.homeshop.Services.OrderService;
import com.nzby.homeshop.Services.UserService;
import com.nzby.homeshop.Utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile/courier")
public class CourierAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CourierAuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CourierActivityService courierActivityService;

    @Autowired
    private CourierService courierService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Аутентификация
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = userService.findByEmail(loginRequest.getEmail());
            if (user == null || user.getRole() != Role.COURIER) {
                logger.warn("Попытка входа с email {}: пользователь не курьер", loginRequest.getEmail());
                response.put("success", false);
                response.put("message", "Доступ разрешен только для курьеров");
                return ResponseEntity.status(403).body(response);
            }

            Courier courier = user.getCourier();
            if (courier != null) {
                courierActivityService.logActivity(courier, ActivityType.LOGIN, "Курьер вошел в систему");
            } else {
                logger.warn("Курьер не найден для пользователя с email {}", loginRequest.getEmail());
            }
            // Генерация JWT
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            logger.info("Успешная авторизация курьера: {}", loginRequest.getEmail());
            response.put("success", true);
            response.put("token", token);
            response.put("message", "Успешная авторизация");
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("Неверные учетные данные для email: {}", loginRequest.getEmail());
            response.put("success", false);
            response.put("message", "Неверные email или пароль");
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            logger.error("Ошибка сервера при авторизации email: {}", loginRequest.getEmail(), e);
            response.put("success", false);
            response.put("message", "Ошибка сервера");
            return ResponseEntity.status(500).body(response);
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = null;
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }
            User user = userService.findByEmail(email);
            if (user == null || user.getRole() != Role.COURIER) {
                logger.warn("Попытка выхода с email {}: пользователь не курьер", email);
                response.put("success", false);
                response.put("message", "Доступ разрешен только для курьеров");
                return ResponseEntity.status(403).body(response);
            }

            // Логирование активности LOGOUT
            Courier courier = user.getCourier();
            if (courier != null) {
                courierActivityService.logActivity(courier, ActivityType.LOGOUT, "Курьер вышел из системы");
            } else {
                logger.warn("Курьер не найден для пользователя с email {}", email);
            }
            logger.info("Запрос на выход из аккаунта получен");
            response.put("success", true);
            response.put("message", "Выход выполнен успешно");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Ошибка при выполнении выхода: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка при выходе из аккаунта");
            return ResponseEntity.status(500).body(response);
        }
    }
    @PostMapping("/activity")
    public ResponseEntity<Map<String, Object>> updateActivity(@RequestBody ActivityRequest activityRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = null;
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            if (email == null) {
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            User user = userService.findByEmail(email);
            if (user == null || user.getRole() != Role.COURIER || user.getCourier() == null) {
                logger.warn("Попытка обновления активности с email {}: пользователь не курьер", email);
                response.put("success", false);
                response.put("message", "Доступ разрешен только для курьеров");
                return ResponseEntity.status(403).body(response);
            }

            Courier courier = user.getCourier();
            ActivityType activityType;
            if ("READY".equalsIgnoreCase(activityRequest.getActivityType())) {
                courier.setStatus(AvailabilityStatus.ONLINE);
                activityType = ActivityType.READY;
            } else if ("OFFLINE".equalsIgnoreCase(activityRequest.getActivityType())) {
                courier.setStatus(AvailabilityStatus.OFFLINE);
                activityType = ActivityType.OFFLINE;
            } else {
                response.put("success", false);
                response.put("message", "Неподдерживаемый тип активности");
                return ResponseEntity.status(400).body(response);
            }

            courier.setLastActive(LocalDateTime.now());
            courierService.save(courier);

            CourierActivity activity = new CourierActivity();
            activity.setCourier(courier);
            activity.setActivityType(activityType);
            activity.setDetails(activityRequest.getDetails());
            activity.setTimestamp(LocalDateTime.now());
            courierActivityService.save(activity);

            logger.info("Курьер {} обновил активность: {}", email, activityRequest.getActivityType());
            response.put("success", true);
            response.put("message", "Активность обновлена");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении активности для email {}: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка сервера");
            return ResponseEntity.status(500).body(response);
        }
    }
    @GetMapping("/processed")
    @PreAuthorize("hasAuthority('ROLE_COURIER')")
    public ResponseEntity<List<OrderDTO>> getProcessedOrders() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).build();
        }
        List<OrderDTO> processedOrders = orderService.getProcessedOrdersDTO(courier);
        return ResponseEntity.ok(processedOrders);
    }

    @GetMapping("/processed/all")
    @PreAuthorize("hasAuthority('ROLE_COURIER')")
    public ResponseEntity<List<OrderDTO>> getAllProcessedOrders() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).build();
        }
        List<OrderDTO> processedOrders = orderService.getAllProcessedOrdersDTO(courier);
        return ResponseEntity.ok(processedOrders);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ROLE_COURIER')")
    public ResponseEntity<List<OrderDTO>> getActiveOrders() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).build();
        }
        List<OrderDTO> activeOrders = orderService.getActiveOrdersDTO(courier);
        return ResponseEntity.ok(activeOrders);
    }

    @GetMapping("/active/all")
    @PreAuthorize("hasAuthority('ROLE_COURIER')")
    public ResponseEntity<List<OrderDTO>> getAllActiveOrders() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).build();
        }
        List<OrderDTO> activeOrders = orderService.getAllActiveOrdersDTO(courier);
        return ResponseEntity.ok(activeOrders);
    }

    @GetMapping("/assigned/all")
    public ResponseEntity<List<AssignedOrderDTO>> getAllAssignedOrders() {
        List<AssignedOrderDTO> assignedOrders = orderService.getAllAssignedOrders();
        return ResponseEntity.ok(assignedOrders);
    }
    @GetMapping("/delivered/all")
    public ResponseEntity<List<OrderDTO>> getAlldeliveredOrders() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).build();
        }
        List<OrderDTO> activeOrders = orderService.getAllDeliveredOrdersDTO(courier);
        return ResponseEntity.ok(activeOrders);
    }
    @GetMapping("/assigned")
    public ResponseEntity<List<OrderDTO>> getAssignedOrders() {
        // Извлечение текущего пользователя из контекста безопасности (JWT токен)
        User user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        // Проверка наличия связанного объекта Courier
        Courier courier = user.getCourier();
        if (courier == null) {
            return ResponseEntity.status(400).body(null); // Bad Request
        }

        // Получение и маппинг назначенных заказов в DTO
        List<OrderDTO> assignedOrders = orderService.getAllAssignedOrdersForCourier(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().toString(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(assignedOrders);
    }
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = null;
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            if (email == null) {
                response.put("success", false);
                response.put("message", "Требуется аутентификация");
                return ResponseEntity.status(401).body(response);
            }

            User courierUser = userService.findByEmail(email);
            if (courierUser == null || courierUser.getRole() != Role.COURIER || courierUser.getCourier() == null) {
                logger.warn("Попытка получения заказа с email {}: пользователь не курьер", email);
                response.put("success", false);
                response.put("message", "Доступ разрешен только для курьеров");
                return ResponseEntity.status(403).body(response);
            }

            Order order = orderService.findById(orderId);
            if (order == null) {
                logger.warn("Заказ с ID {} не найден", orderId);
                response.put("success", false);
                response.put("message", "Заказ не найден");
                return ResponseEntity.status(404).body(response);
            }

            Courier courier = courierUser.getCourier();
            if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
                logger.warn("Курьер {} пытался получить доступ к заказу {}, который ему не назначен", email, orderId);
                response.put("success", false);
                response.put("message", "Доступ к заказу запрещен");
                return ResponseEntity.status(403).body(response);
            }

            OrderDetailsDTO orderDetailsDTO = new OrderDetailsDTO();
            orderDetailsDTO.setOrderId(order.getId());
            User customer = order.getUser();
            orderDetailsDTO.setCustomerName(customer.getName() != null ? customer.getName() : "Не указано");
            orderDetailsDTO.setCustomerPhone(customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "Не указано");
            orderDetailsDTO.setOrderDate(order.getOrderDate());
            orderDetailsDTO.setStatus(order.getStatus());
            orderDetailsDTO.setTotalAmount(order.getTotalAmount());
            orderDetailsDTO.setPaymentMethod(order.getPaymentMethod());
            orderDetailsDTO.setPaymentStatus(order.getPaymentStatus());
            orderDetailsDTO.setTotalWeightInGrams(order.getTotalWeightInGrams());
            orderDetailsDTO.setDeliveryAddress(order.getAddress().getStreetAddress());
            orderDetailsDTO.setOrderItems(order.getOrderItems().stream().map(item -> {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                return itemDTO;
            }).collect(Collectors.toList()));

            response.put("success", true);
            response.put("data", orderDetailsDTO);
            logger.info("Курьер {} получил детали заказа {}", email, orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Ошибка при получении заказа {} для email {}: {}", orderId, e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка сервера");
            return ResponseEntity.status(500).body(response);
        }
    }
    @GetMapping(value = "/order/{id}/status")
    @ResponseBody
    @Transactional
    public ResponseEntity<OrderStatusDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {
        try {
            // Извлечение email из SecurityContextHolder
            String email = null;
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            // Проверка аутентификации
            if (email == null) {
                logger.warn("Попытка доступа к заказу {} без аутентификации", id);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется аутентификация");
            }

            User courierUser = userService.findByEmail(email);
            if (courierUser == null || courierUser.getRole() != Role.COURIER || courierUser.getCourier() == null) {
                logger.warn("Попытка получения заказа с email {}: пользователь не курьер", email);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ разрешен только для курьеров");
            }

            Order order = orderService.findById(id);
            if (order == null) {
                logger.warn("Заказ с ID {} не найден", id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ #" + id + " не найден");
            }

            Courier courier = courierUser.getCourier();
            if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
                logger.warn("Курьер {} пытался получить доступ к заказу {}, который ему не назначен", email, id);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ к заказу запрещен");
            }

            order.setStatus(OrderStatus.DELIVERED);
            order.setOrderDate(LocalDateTime.now());
            orderService.save(order); // Сохранение заказа

            // Формирование ответа
            OrderStatusDTO responseDTO = new OrderStatusDTO(order.getId(), order.getStatus());
            logger.info("Курьер {} установил статус DELIVERED для заказа #{}", email, id);
            return ResponseEntity.ok(responseDTO);
        } catch (ResponseStatusException e) {
            logger.error("Ошибка при обработке заказа {}: {}", id, e.getReason());
            throw e;
        } catch (Exception e) {
            logger.error("Ошибка при получении статуса заказа {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка сервера");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(Authentication authentication) {
        try {
            // Проверяем, аутентифицирован ли пользователь
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Неавторизованный доступ к профилю");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Неавторизован"));
            }

            // Получаем текущего пользователя
            User user = userService.getCurrentUser();
            if (user == null) {
                logger.error("Текущий пользователь не найден");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Пользователь не найден"));
            }

            // Формируем данные профиля
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("name", user.getName() + " " + user.getSurname()); // Объединяем имя и фамилию
            profileData.put("email", user.getEmail());
            profileData.put("phone", null); // Если поле phone есть, замените null на user.getPhone()
            profileData.put("avatar", null); // Если поле avatar есть, замените null на user.getAvatar()

            // Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Профиль успешно загружен");
            response.put("data", profileData);

            logger.info("Профиль успешно загружен для пользователя: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Ошибка при загрузке профиля: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при загрузке профиля"));
        }
    }
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}
