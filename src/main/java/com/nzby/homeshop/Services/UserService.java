package com.nzby.homeshop.Services;

import com.nzby.homeshop.DTO.UpdateProfileRequest;
import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.POJO.Enum.ActivityType;
import com.nzby.homeshop.Repository.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nzby.homeshop.POJO.Enum.Role;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

   // Получение текущего аутентифицированного пользователя
   public User getCurrentUser() {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
           return null;
       }
       String username = ((UserDetails) authentication.getPrincipal()).getUsername();
       return userRepository.findByEmail(username)
               .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с email: " + username));
   }

    // Регистрация нового пользователя
    public User registerUser(String email, String password, String name, Role role) {
        // Проверка, существует ли пользователь с таким email
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        // Создание нового пользователя
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Шифрование пароля
        newUser.setName(name);
        newUser.setRole(role != null ? role : Role.USER); // Установка роли по умолчанию, если не указана
        newUser.setEnabled(false); // По умолчанию email не подтвержден

        return userRepository.save(newUser);
    }

    // Обновление данных пользователя
    public User updateUser(Long userId, String name, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с ID: " + userId));

        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }
        if (email != null && !email.isEmpty()) {
            // Проверка, не используется ли новый email другим пользователем
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Email уже используется другим пользователем");
            }
            user.setEmail(email);
            user.setEnabled(false); // Требуется повторное подтверждение email
        }

        return userRepository.save(user);
    }

    // Подтверждение email пользователя
    public void confirmEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с ID: " + userId));
        user.setEnabled(true);
        userRepository.save(user);
    }

    // Смена пароля пользователя
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с ID: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Получение пользователя по ID
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с ID: " + userId));
    }

    // Удаление пользователя
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден с ID: " + userId));
        userRepository.delete(user);
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email не найден: " + email));
    }

    public Page<User> findUsers(String email, Role role, PageRequest pageable) {
        return userRepository.findUsers(email, role, pageable);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void updateUserRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setRole(role);
        userRepository.save(user);
    }

    public void toggleUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void addToWishlist(String email, Long productId) {
        User user = findByEmail(email);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));
        if (wishlistRepository.findByUserEmailAndProductId(email, productId).isEmpty()) {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProduct(product);
            wishlist.setAddedAt(LocalDateTime.now());
            wishlistRepository.save(wishlist);
        }
    }

    @Transactional
    public void removeFromWishlist(String email, Long productId) {
        wishlistRepository.findByUserEmailAndProductId(email, productId)
                .ifPresent(wishlistRepository::delete);
    }

    public List<Product> getFavoriteProducts(String email) {
        return wishlistRepository.findByUserEmail(email)
                .stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }

    public List<Long> getWishlistProductIds(String email) {
        return wishlistRepository.findProductIdsByUserEmail(email);
    }
    public boolean isAuthenticated(Authentication authentication) {
        User user = null;
        UserDetails userDetails = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                userDetails = (UserDetails) principal;
                user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (user == null) {
                    logger.warn("Пользователь с email {} не найден в базе данных", userDetails.getUsername());
                }
            }
        }

        return userDetails != null;
    }

    @Transactional
    public void updateUserProfile(User user, String name, String surname, String phone) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (surname == null || surname.trim().isEmpty()) {
            throw new IllegalArgumentException("Фамилия не может быть пустой");
        }
        if (phone == null || !phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Неверный формат номера телефона");
        }
        user.setName(name);
        user.setSurname(surname);
        user.setPhoneNumber(phone);
        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(@Valid UpdateProfileRequest request) throws IllegalArgumentException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> optionalUser = userRepository.findByEmail(username);
        if (!optionalUser.isPresent()) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        User user = optionalUser.get();
        // Нормализация имени и фамилии
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(capitalize(request.getName().trim()));
        }
        if (request.getSurname() != null && !request.getSurname().trim().isEmpty()) {
            user.setSurname(capitalize(request.getSurname().trim()));
        }
        // Форматирование номера телефона
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String phone = request.getPhoneNumber().trim();
            if (phone.startsWith("8")) {
                phone = "+7" + phone.substring(1);
            }
            user.setPhoneNumber(phone);
        } else {
            user.setPhoneNumber(null);
        }

        userRepository.save(user);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.toLowerCase().split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return capitalized.toString().trim();
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

}