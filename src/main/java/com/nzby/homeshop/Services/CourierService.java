package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.CourierActivity;
import com.nzby.homeshop.POJO.Enum.ActivityType;
import com.nzby.homeshop.POJO.Enum.AvailabilityStatus;
import com.nzby.homeshop.POJO.Enum.OrderStatus;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.CourierActivityRepository;
import com.nzby.homeshop.Repository.CourierRepository;
import com.nzby.homeshop.Repository.OrderRepository;
import com.nzby.homeshop.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class CourierService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zА-Яа-я]{2,50}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    @Autowired
    private CourierActivityRepository courierActivityRepository;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User createCourier(String email, String password, String name, String surname, String phoneNumber) {
        // Валидация
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Некорректный email");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email уже используется");
        }
        if (password == null || password.isEmpty()) {
            password = generatePassword();
        }
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Имя: 2-50 букв");
        }
        if (surname == null || !NAME_PATTERN.matcher(surname).matches()) {
            throw new IllegalArgumentException("Фамилия: 2-50 букв");
        }
        if (phoneNumber != null && !phoneNumber.isEmpty() && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Неверный формат телефона: должен начинаться с +7 или 8 и содержать 10 цифр");
        }

        // Создание пользователя
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setSurname(surname);
        user.setPhoneNumber(phoneNumber);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.COURIER);
        user.setEnabled(true);

        user = userRepository.save(user);

        // Создание курьера
        Courier courier = new Courier();
        courier.setUser(user);
        courier.setStatus(AvailabilityStatus.OFFLINE);
        courier.setLastActive(null);
        courier.setStatus(AvailabilityStatus.OFFLINE);

        courierRepository.save(courier);

        System.out.println("Сгенерированный пароль для " + email + ": " + password);

        return user;
    }

    public String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    public void updateCourier(Courier courier) {
        courier.setLastActive(LocalDateTime.now());
        courierRepository.save(courier);
    }

    public void logActivity(Courier courier, ActivityType activityType, String details) {
        CourierActivity activity = new CourierActivity();
        activity.setCourier(courier);
        activity.setActivityType(ActivityType.valueOf(activityType.toString()));
        activity.setDetails(details);
        activity.setTimestamp(LocalDateTime.now());
        courierActivityRepository.save(activity);
    }

    public List<CourierActivity> getCourierActivity(Courier courier, LocalDateTime since) {
        return courierActivityRepository.findByCourierAndTimestampAfterOrderByTimestampDesc(courier, since);
    }

    public void completeOrder(Long orderId, Courier courier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        if (order.getCourier() == null || !order.getCourier().getId().equals(courier.getId())) {
            throw new RuntimeException("Заказ не принадлежит курьеру");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logActivity(courier, ActivityType.ORDER_COMPLETED, "Заказ #" + orderId);
    }

    public List<Order> getCompletedOrders(Courier courier) {
        return orderRepository.findByCourierAndStatus(courier, OrderStatus.DELIVERED);
    }

    public void save(Courier courier) {
        courierRepository.save(courier);
    }

    public List<Courier> findOnlineCouriers() {
        return courierRepository.findByStatus(AvailabilityStatus.ONLINE);
    }

    public Courier findById(Long courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Курьер не найден id: " + courierId));
    }
}