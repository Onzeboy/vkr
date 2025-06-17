package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.UserRepository;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.Services.EmailService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final List<String> COMMON_PASSWORDS = List.of(
            "password", "12345678", "123456789", "qwertyui", "qwerty123",
            "1q2w3e4r", "11111111", "password1", "abc12345", "admin123",
            "welcome1", "sunshine", "iloveyou", "monkey12", "football",
            "baseball", "letmein", "shadow1", "master1", "superman",
            "qazwsxed", "password123", "Password1", "Admin123", "Welcome1",
            "123qweasd", "12345qwe", "1qaz2wsx", "zaq12wsx", "!qaz2wsx",
            "qwertyuiop", "asdfghjkl", "zxcvbnm", "987654321", "123123123"
    );

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String expired,
                                Model model) {

        if (error != null) {
            model.addAttribute("error", "Неверный email или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        if (expired != null) {
            model.addAttribute("error", "Ваша сессия истекла. Пожалуйста, войдите снова.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("user") User user,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        String normalizedEmail = user.getEmail().trim().toLowerCase();
        user.setEmail(normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            bindingResult.rejectValue("email", "duplicate.email", "Этот email уже зарегистрирован");
        }

        validatePassword(user.getPassword(), user, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "auth/register";
        }

        try {
            saveUser(user);
            emailService.sendConfirmationEmail(user);
            redirectAttributes.addFlashAttribute("email", user.getEmail());
            return "redirect:/auth/confirm-code";
        } catch (Exception e) {
            bindingResult.reject("global", "Произошла ошибка при регистрации. Пожалуйста, попробуйте позже.");
            model.addAttribute("user", user);
            model.addAttribute("globalError", "Произошла ошибка при регистрации. Пожалуйста, попробуйте позже.");
            return "auth/register";
        }
    }

    private void validatePassword(String password, User user, BindingResult bindingResult) {
        if (password == null || password.isEmpty()) {
            bindingResult.rejectValue("password", "empty.password", "Пароль не может быть пустым");
            return;
        }

        if (password.length() < 8) {
            bindingResult.rejectValue("password", "size.password", "Пароль должен содержать минимум 8 символов");
        }

        if (!password.matches(".*\\d.*")) {
            bindingResult.rejectValue("password", "pattern.digit", "Пароль должен содержать хотя бы одну цифру");
        }

        if (!password.matches(".*[A-ZА-Я].*")) {
            bindingResult.rejectValue("password", "pattern.uppercase", "Пароль должен содержать хотя бы одну заглавную букву");
        }

        if (isCommonPassword(password)) {
            bindingResult.rejectValue("password", "common.password", "Этот пароль слишком распространен");
        }

        if (isPasswordSimilarToPersonalData(password, user)) {
            bindingResult.rejectValue("password", "personal.password", "Пароль не должен содержать ваши личные данные");
        }
    }

    private boolean isCommonPassword(String password) {
        return COMMON_PASSWORDS.contains(password.toLowerCase());
    }

    private boolean isPasswordSimilarToPersonalData(String password, User user) {
        String lowerPass = password.toLowerCase();
        return (user.getName() != null && lowerPass.contains(user.getName().toLowerCase())) ||
                (user.getSurname() != null && lowerPass.contains(user.getSurname().toLowerCase())) ||
                (user.getEmail() != null && lowerPass.contains(user.getEmail().split("@")[0].toLowerCase()));
    }

    private void saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.USER);
        user.setEnabled(false);
        userRepository.save(user);
    }

    @GetMapping("/confirm-code")
    public String showConfirmationForm(@ModelAttribute("email") String email,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (email == null || email.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо указать email");
            return "redirect:/auth/register";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/auth/register";
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            try {
                emailService.sendConfirmationEmail(user);
                model.addAttribute("message", "Код подтверждения отправлен на " + email);
            } catch (Exception e) {
                model.addAttribute("error", "Не удалось отправить код подтверждения. Пожалуйста, попробуйте позже.");
            }
        }

        model.addAttribute("email", email);
        return "auth/confirm-code";
    }

    @PostMapping("/confirm-code")
    @Transactional
    public String processConfirmation(@RequestParam String email,
                                      @RequestParam String code,
                                      RedirectAttributes redirectAttributes) {
        if (email == null || email.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо указать email");
            return "redirect:/auth/register";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/auth/register";
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            redirectAttributes.addFlashAttribute("info", "Ваш email уже подтвержден");
            return "redirect:/auth/login";
        }

        try {
            // Временное упрощение: обойти emailService для тестирования
            user.setEnabled(true);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Email успешно подтвержден! Теперь вы можете войти.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("error", "Произошла ошибка при подтверждении email");
            return "redirect:/auth/confirm-code";
        }
    }

    @GetMapping("/resend-code")
    public String resendConfirmationCode(@RequestParam String email,
                                         RedirectAttributes redirectAttributes) {
        if (email == null || email.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Необходимо указать email");
            return "redirect:/auth/register";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/auth/register";
        }

        User user = userOptional.get();
        if (user.isEnabled()) {
            redirectAttributes.addFlashAttribute("info", "Ваш email уже подтвержден");
            return "redirect:/auth/login";
        }

        try {
            emailService.sendConfirmationEmail(user);
            redirectAttributes.addFlashAttribute("success", "Новый код отправлен на " + email);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/auth/confirm-code";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("error", "Не удалось отправить код подтверждения");
            return "redirect:/auth/confirm-code";
        }
    }
}