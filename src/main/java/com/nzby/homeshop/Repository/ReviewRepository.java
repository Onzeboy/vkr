package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId ORDER BY (SELECT COALESCE(SUM(v.vote), 0) FROM ReviewVote v WHERE v.review.id = r.id) DESC")
    Page<Review> findByProductIdOrderByVoteScoreDesc(@Param("productId") Long productId, Pageable pageable);
}