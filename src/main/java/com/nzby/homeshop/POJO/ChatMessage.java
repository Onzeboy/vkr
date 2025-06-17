package com.nzby.homeshop.POJO;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_ticket_id", columnList = "ticket_id"),
        @Index(name = "idx_chat_messages_sender_id", columnList = "sender_id"),
        @Index(name = "idx_chat_messages_timestamp", columnList = "timestamp"),
        @Index(name = "idx_chat_messages_status", columnList = "status"),
        @Index(name = "idx_chat_messages_ticket_timestamp", columnList = "ticket_id, timestamp")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Size(max = 1000, message = "Содержимое сообщения не должно превышать 1000 символов")
    @Column(name = "content", nullable = true, length = 1000)
    private String content;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatImage> images = new ArrayList<>();

    // Перечисление статусов сообщения
    public enum MessageStatus {
        SENT, DELIVERED, ADMIN_SENT
    }

    // Конструкторы
    public ChatMessage() {}

    public ChatMessage(SupportTicket ticket, User sender, String content) {
        this.ticket = ticket;
        this.sender = sender;
        this.content = content;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }


    public List<ChatImage> getImages() {
        return images;
    }

    public void setImages(List<ChatImage> images) {
        this.images = images;
    }

    public void addImage(ChatImage image) {
        images.add(image);
        image.setMessage(this);
    }

    public void removeImage(ChatImage image) {
        images.remove(image);
        image.setMessage(null);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", ticketId=" + (ticket != null ? ticket.getId() : null) +
                ", senderId=" + (sender != null ? sender.getId() : null) +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}