package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.Services.ChatService;
import com.nzby.homeshop.Services.OrderService;
import com.nzby.homeshop.Services.ProductService;
import com.nzby.homeshop.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @GetMapping("/chat")
    public String showChat(Model model, Authentication authentication) {
        User user = userService.getCurrentUser();
        if (user == null) {
            log.error("Current user not found");
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("currentUserEmail", null);
            return "redirect:/auth/login";
        }
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("currentUserEmail", user.getEmail());
        return "chats";
    }

    @GetMapping("/api/chat/tickets")
    @ResponseBody
    public ResponseEntity<List<SupportTicket>> getTickets(Authentication authentication) {
        User user = userService.getCurrentUser();
        if (user == null) {
            log.error("Current user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }
        List<SupportTicket> tickets = chatService.getTicketsForUser(user).stream()
                .map(ticket -> {
                    SupportTicket simplified = new SupportTicket();
                    simplified.setId(ticket.getId());
                    simplified.setCreatedAt(ticket.getCreatedAt());
                    simplified.setStatus(ticket.getStatus());
                    return simplified;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/api/chat/tickets")
    @ResponseBody
    public ResponseEntity<?> createTicket(Authentication authentication) {
        User user = userService.getCurrentUser();
        if (user == null) {
            log.error("Current user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current user not found");
        }
        List<SupportTicket> openTickets = chatService.getTicketsForUser(user).stream()
                .filter(t -> !t.getStatus().equals(SupportTicket.TicketStatus.CLOSED))
                .collect(Collectors.toList());
        if (!openTickets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("У вас уже есть не закрытая заявка");
        }
        SupportTicket ticket = chatService.createTicket(user);
        SupportTicket simplified = new SupportTicket();
        simplified.setId(ticket.getId());
        simplified.setCreatedAt(ticket.getCreatedAt());
        simplified.setStatus(ticket.getStatus());
        return ResponseEntity.ok(simplified);
    }

    @GetMapping("/api/chat/history/{ticketId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long ticketId, Authentication authentication) {
        User user = userService.getCurrentUser();
        if (user == null) {
            log.error("Current user not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }
        SupportTicket ticket = chatService.getTicketsForUser(user).stream()
                .filter(t -> t.getId().equals(ticketId))
                .findFirst()
                .orElse(null);
        if (ticket == null) {
            log.error("Ticket not found or access denied for ticketId: {}", ticketId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
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
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(
            @RequestParam("ticketId") Long ticketId,
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User user = userService.getCurrentUser();
        return chatService.sendMessage(ticketId, content, files, user);
    }

}