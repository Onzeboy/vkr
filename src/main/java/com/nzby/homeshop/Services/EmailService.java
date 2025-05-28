package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UserRepository userRepository; // Для обновления enabled

    @Value("${spring.mail.username}")
    private String from;

    private static final int CODE_LENGTH = 6;

    private String lastGeneratedCode; // Временное хранение кода (только для текущей сессии)

    public void sendConfirmationEmail(User user) {
        // Генерируем новый код
        String code = generateRandomCode();
        lastGeneratedCode = code; // Сохраняем код в памяти (не в базе)

        // Отправляем email
        String subject = "Код подтверждения";
        String message = "Здравствуйте, " + (user.getName() != null ? user.getName() : "") + "!\n\n" +
                "Ваш код подтверждения: " + code + "\n\n" +
                "Введите этот код на странице подтверждения для активации вашего аккаунта.";

        sendSimpleEmail(user.getEmail(), subject, message);
    }

    public boolean confirmEmail(String code, User user) {
        // Проверяем, совпадает ли введённый код с последним сгенерированным
        if (lastGeneratedCode != null && lastGeneratedCode.equals(code)) {
            // Активируем пользователя
            user.setEnabled(true);
            userRepository.save(user);
            lastGeneratedCode = null; // Очищаем код после успешного подтверждения
            return true;
        }
        return false;
    }

    public void sendSimpleEmail(String toAddress, String subject, String message) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(from);
        simpleMailMessage.setTo(toAddress);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(message);
        emailSender.send(simpleMailMessage);
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}