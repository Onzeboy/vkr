package com.nzby.homeshop.POJO;

import jakarta.persistence.*;

@Entity
@Table(name = "review_images")
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Конструкторы
    public ReviewImage(String path) {}

    public ReviewImage(Review review, String filePath) {
        this.review = review;
        this.filePath = filePath;
    }

    public ReviewImage() {

    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "ReviewImage{" +
                "id=" + id +
                ", reviewId=" + (review != null ? review.getId() : null) +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}