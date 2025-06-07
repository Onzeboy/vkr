package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Services.ChatService;
import com.nzby.homeshop.Services.ReviewService;
import com.nzby.homeshop.Services.UserService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class ChatAdminController {
    private static final Logger log = LoggerFactory.getLogger(ChatAdminController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @GetMapping("/chat")
    public String showAdminDashboard(Model model) {
        List<SupportTicket> openTickets = chatService.getOpenTickets(10).stream()
                .map(ticket -> {
                    SupportTicket simplified = new SupportTicket();
                    simplified.setId(ticket.getId());
                    simplified.setCreatedAt(ticket.getCreatedAt());
                    simplified.setStatus(ticket.getStatus());
                    simplified.setUser(ticket.getUser());
                    return simplified;
                })
                .collect(Collectors.toList());

        model.addAttribute("openTickets", openTickets);
        return "admin/chats-list";
    }

    /**
     * Отображает детальную страницу чата по ID тикета.
     */
    @GetMapping("/chat/{ticketId}")
    public String showChatDetails(@PathVariable Long ticketId, Model model) {
        SupportTicket ticket = chatService.getTicketById(ticketId);
        if (ticket == null) {
            log.error("Ticket not found for ticketId: {}", ticketId);
            return "redirect:/admin";
        }

        List<ChatMessage> messages = chatService.getMessagesForTicket(ticket).stream()
                .map(message -> {
                    ChatMessage simplified = new ChatMessage();
                    simplified.setId(message.getId());
                    simplified.setContent(message.getContent());
                    simplified.setTimestamp(message.getTimestamp());
                    simplified.setStatus(message.getStatus());
                    simplified.setImages(message.getImages().stream()
                            .map(image -> {
                                ChatImage simplifiedImage = new ChatImage();
                                simplifiedImage.setId(image.getId());
                                simplifiedImage.setFilePath(image.getFilePath());
                                return simplifiedImage;
                            })
                            .collect(Collectors.toList()));
                    return simplified;
                })
                .collect(Collectors.toList());

        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", messages);
        return "admin/adminchat";
    }

    /**
     * Изменяет статус тикета (например, с "Открыт" на "Закрыт").
     */
    @PostMapping("/api/tickets/{ticketId}/update-status")
    @ResponseBody
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam("status") String status) {
        try {
            SupportTicket ticket = chatService.getTicketById(ticketId);
            if (ticket == null) {
                log.error("Ticket not found for ticketId: {}", ticketId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Ticket not found"));
            }

            SupportTicket.TicketStatus newStatus;
            try {
                newStatus = SupportTicket.TicketStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid status value: {}", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid status value"));
            }

            if (ticket.getStatus() == newStatus) {
                log.info("Status unchanged for ticketId {}: {}", ticketId, newStatus);
                return ResponseEntity.ok(Map.of("message", "Status unchanged", "status", newStatus.toString()));
            }

            chatService.updateTicketStatus(ticketId, newStatus);
            log.info("Ticket status updated for ticketId {} to {}", ticketId, newStatus);
            return ResponseEntity.ok(Map.of("message", "Status updated", "status", newStatus.toString()));
        } catch (Exception e) {
            log.error("Error updating ticket status for ticketId {}: {}", ticketId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error updating status"));
        }
    }

    /**
     * Отправляет сообщение от админа в чат.
     */
    @PostMapping("/api/tickets/{ticketId}/send")
    @ResponseBody
    public ResponseEntity<?> sendAdminMessage(
            @PathVariable Long ticketId,
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        try {
            SupportTicket ticket = chatService.getTicketById(ticketId);
            if (ticket == null) {
                log.error("Тикет не найден для ticketId: {}", ticketId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Тикет не найден"));
            }
            if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
                log.warn("Попытка отправки сообщения в закрытый тикет ticketId: {}", ticketId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Нельзя отправить сообщение в закрытый тикет"));
            }
            User admin = userService.getCurrentUser();
            if (admin == null) {
                log.error("Неавторизованный доступ для отправки сообщения админом");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Неавторизован"));
            }
            ResponseEntity<?> response = chatService.sendMessage(ticketId, content, files, admin);
            if (response.getStatusCode() != HttpStatus.OK) {
                return response;
            }
            ChatMessage message = (ChatMessage) response.getBody();
            ChatMessage simplified = new ChatMessage();
            simplified.setId(message.getId());
            simplified.setContent(message.getContent());
            simplified.setTimestamp(message.getTimestamp());
            return ResponseEntity.ok(simplified);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения админом для ticketId {}: {}", ticketId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при отправке сообщения"));
        }
    }

    @GetMapping("/ticket-history")
    public String showTicketHistory(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<SupportTicket> ticketPage = chatService.getAllTickets(PageRequest.of(page, size));
        List<SupportTicket> tickets = ticketPage.getContent().stream()
                .map(ticket -> {
                    SupportTicket simplified = new SupportTicket();
                    simplified.setId(ticket.getId());
                    simplified.setCreatedAt(ticket.getCreatedAt());
                    simplified.setStatus(ticket.getStatus());
                    simplified.setUser(ticket.getUser());
                    return simplified;
                })
                .collect(Collectors.toList());

        model.addAttribute("tickets", tickets);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        return "admin/chatshistory";
    }
    @DeleteMapping("/api/tickets/{ticketId}")
    @ResponseBody
    public ResponseEntity<?> deleteTicket(@PathVariable Long ticketId) {
        try {
            SupportTicket ticket = chatService.getTicketById(ticketId);
            if (ticket == null) {
                log.error("Тикет не найден для ticketId: {}", ticketId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Тикет не найден"));
            }
            if (ticket.getStatus() != SupportTicket.TicketStatus.CLOSED) {
                log.warn("Попытка удаления незакрытого тикета ticketId: {}", ticketId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Нельзя удалить незакрытый тикет"));
            }
            User admin = userService.getCurrentUser();
            if (admin == null) {
                log.error("Неавторизованный доступ для удаления тикета");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Неавторизован"));
            }
            chatService.deleteTicketById(ticketId);
            log.info("Тикет удалён для ticketId: {}", ticketId);
            return ResponseEntity.ok(Map.of("message", "Тикет удалён"));
        } catch (Exception e) {
            log.error("Ошибка при удалении тикета для ticketId {}: {}", ticketId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ошибка при удалении тикета"));
        }
    }
}