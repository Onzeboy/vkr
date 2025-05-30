package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}