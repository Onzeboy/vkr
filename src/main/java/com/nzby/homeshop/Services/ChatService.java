package com.nzby.homeshop.Services;

import com.nzby.homeshop.Controllers.ChatController;
import com.nzby.homeshop.POJO.ChatImage;
import com.nzby.homeshop.POJO.ChatMessage;
import com.nzby.homeshop.POJO.SupportTicket;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.ChatImageRepository;
import com.nzby.homeshop.Repository.ChatMessageRepository;
import com.nzby.homeshop.Repository.SupportTicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatImageRepository chatImageRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    public SupportTicket createTicket(User user) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setStatus(SupportTicket.TicketStatus.OPEN);
        return ticketRepository.save(ticket);
    }

    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<SupportTicket> getTicketsForUser(User user) {
        if (user.getRole() == com.nzby.homeshop.POJO.Enum.Role.ADMIN) {
            return ticketRepository.findAll();
        }
        return ticketRepository.findByUser(user);
    }

    public List<ChatMessage> getMessagesForTicket(SupportTicket ticket) {
        return chatMessageRepository.findByTicket(ticket);
    }

    public List<ChatMessage> getMessagesForUser(User user) {
        return null;
    }

    @Transactional
    public ResponseEntity<?> sendMessage(Long ticketId, String content, MultipartFile[] files, User sender) {
        // Validate sender
        if (sender == null) {
            log.error("Current user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current user not found");
        }


        boolean isAdmin = sender.getAuthorities().contains("ADMIN");

        // Validate ticket
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElse(null); // Используем репозиторий для проверки всех тикетов (админ видит все)
        if (ticket == null) {
            log.error("Ticket not found for ticketId: {}", ticketId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ticket not found");
        }

        // Для пользователей проверяем доступ к тикету
        if (!isAdmin && getTicketsForUser(sender).stream().noneMatch(t -> t.getId().equals(ticketId))) {
            log.error("Access denied for ticketId: {} by user {}", ticketId, sender.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        // Validate file count
        if (files != null && files.length > 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Можно прикрепить не более 2 фотографий");
        }

        // Validate content
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Сообщение не может быть пустым");
        }

        // Create message
        ChatMessage message = new ChatMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setStatus(isAdmin ? ChatMessage.MessageStatus.ADMIN_SENT : ChatMessage.MessageStatus.SENT);

        // Save message first to get ID for images
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Process and save images
        BindingResult bindingResult = new BeanPropertyBindingResult(message, "message");
        List<ChatImage> savedImages = processAndSaveImages(savedMessage, files, bindingResult);

        if (bindingResult.hasErrors()) {
            // Rollback message if image processing fails
            chatMessageRepository.delete(savedMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }

        message.setImages(savedImages);
        chatMessageRepository.save(savedMessage);

        // Обновление статуса тикета (только для админа, если указано закрытие)
        if (isAdmin) {
            String statusParam = request.getParameter("status"); // Предполагаем, что статус передается как параметр
            if (statusParam != null && statusParam.equalsIgnoreCase("CLOSED")) {
                ticket.setStatus(SupportTicket.TicketStatus.CLOSED);
                ticketRepository.save(ticket);
            }
        }

        // Create simplified response
        ChatMessage simplified = new ChatMessage();
        simplified.setId(savedMessage.getId());
        simplified.setContent(savedMessage.getContent());
        simplified.setTimestamp(savedMessage.getTimestamp());
        simplified.setStatus(savedMessage.getStatus());
        simplified.setImages(savedImages.stream()
                .map(image -> {
                    ChatImage simplifiedImage = new ChatImage();
                    simplifiedImage.setId(image.getId());
                    simplifiedImage.setFilePath("/uploaded-images/" + image.getFilePath());
                    return simplifiedImage;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(simplified);
    }

    // Предполагаемый метод для получения HttpServletRequest (нужен для доступа к параметрам)
    private HttpServletRequest request;

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Transactional
    public List<ChatImage> processAndSaveImages(ChatMessage message, MultipartFile[] files, BindingResult result) {
        List<ChatImage> images = new ArrayList<>();
        if (files == null || files.length == 0 || (files.length == 1 && files[0].isEmpty())) {
            return images; // No validation error for optional images
        }

        String ticketDir = Paths.get(uploadDir, "tickets", String.valueOf(message.getTicket().getId())).toString();
        Path dirPath = Paths.get(ticketDir);

        try {
            Files.createDirectories(dirPath);
            log.info("Directory created successfully: {}", ticketDir);
            // Verify directory is writable
            if (!Files.isWritable(dirPath)) {
                log.error("Directory is not writable: {}", ticketDir);
                result.rejectValue("images", "images.directory.error", "Папка для изображений недоступна для записи");
                return images;
            }
        } catch (IOException e) {
            log.error("Failed to create directory: {}, error: {}", ticketDir, e.getMessage());
            result.rejectValue("images", "images.directory.error", "Не удалось создать папку для изображений: " + e.getMessage());
            return images;
        }

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                String originalFilename = file.getOriginalFilename();
                log.info("Processing photo {}: name={}, size={}, type={}", i, originalFilename, file.getSize(), contentType);

                // Validate file extension
                String fileExtension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                        : "";
                if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                    log.warn("Invalid file extension for {}: {}", originalFilename, fileExtension);
                    result.rejectValue("images", "images.extension",
                            "Файл " + originalFilename + ": разрешены только файлы с расширениями .jpg, .jpeg, .png");
                    continue;
                }

                // Validate content type
                if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                    log.warn("Invalid file format for {}: {}", originalFilename, contentType);
                    result.rejectValue("images", "images.format",
                            "Файл " + originalFilename + ": разрешены только JPEG и PNG");
                    continue;
                }

                // Validate file size
                if (file.getSize() > MAX_FILE_SIZE) {
                    log.warn("File {} exceeds 5MB limit", originalFilename);
                    result.rejectValue("images", "images.size",
                            "Файл " + originalFilename + ": размер не должен превышать 5 МБ");
                    continue;
                }

                try {
                    // Generate unique filename
                    String fileName = UUID.randomUUID().toString() + fileExtension;
                    Path filePath = dirPath.resolve(fileName);
                    Files.write(filePath, file.getBytes());
                    log.info("Saved photo for ticket {}: {}", message.getTicket().getId(), filePath);

                    // Verify file was written
                    if (!Files.exists(filePath)) {
                        log.error("File was not created: {}", filePath);
                        result.rejectValue("images", "images.upload.error",
                                "Не удалось сохранить файл " + originalFilename);
                        continue;
                    }

                    // Save relative path
                    String relativeFilePath = "tickets/" + message.getTicket().getId() + "/" + fileName;

                    ChatImage image = new ChatImage(message, relativeFilePath);
                    images.add(image);
                    chatImageRepository.save(image);
                    log.info("Saved ChatImage to database: {}", relativeFilePath);
                } catch (IOException e) {
                    log.error("Failed to save photo {} for ticket {}: {}", originalFilename, message.getTicket().getId(), e.getMessage());
                    result.rejectValue("images", "images.upload.error",
                            "Ошибка при загрузке файла " + originalFilename + ": " + e.getMessage());
                }
            }
        }
        return images;
    }

    public SupportTicket getTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
    }

    public void updateTicketStatus(Long ticketId, SupportTicket.TicketStatus newStatus) {
    }
    /**
     * Получает список открытых тикетов с ограничением по количеству.
     * @param limit максимальное количество тикетов
     * @return список открытых тикетов
     */
    public List<SupportTicket> getOpenTickets(int limit) {
        try {
            List<SupportTicket.TicketStatus> allowedStatuses = Arrays.asList(
                    SupportTicket.TicketStatus.OPEN,
                    SupportTicket.TicketStatus.IN_PROGRESS // Добавьте другие статусы по необходимости
            );
            return ticketRepository.findAll()
                    .stream()
                    .filter(ticket -> allowedStatuses.contains(ticket.getStatus()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching open tickets: {}", e.getMessage());
            return List.of(); // Возвращаем пустой список в случае ошибки
        }
    }
}