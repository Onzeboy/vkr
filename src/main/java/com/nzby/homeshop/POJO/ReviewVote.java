package com.nzby.homeshop.POJO;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}))
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vote", nullable = false)
    private Integer vote; // +1 для лайка, -1 для дизлайка


    public ReviewVote() {}

    public ReviewVote(Review review, User user, Integer vote) {
        this.review = review;
        this.user = user;
        this.vote = vote;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getVote() {
        return vote;
    }

    public void setVote(Integer vote) {
        this.vote = vote;
    }

}