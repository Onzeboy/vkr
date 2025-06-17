package com.nzby.homeshop.POJO;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_images", indexes = {
        @Index(name = "idx_chat_images_message_id", columnList = "message_id"),
        @Index(name = "idx_chat_images_file_path", columnList = "file_path")
})
public class ChatImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Конструкторы
    public ChatImage() {}

    public ChatImage(ChatMessage message, String filePath) {
        this.message = message;
        this.filePath = filePath;
    }

    public ChatImage(String path) {
        this.filePath = path;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "ChatImage{" +
                "id=" + id +
                ", messageId=" + (message != null ? message.getId() : null) +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}