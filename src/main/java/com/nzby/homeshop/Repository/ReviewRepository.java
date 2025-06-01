package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}