package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Review;
import com.nzby.homeshop.POJO.ReviewVote;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    ReviewVote findByReviewAndUser(Review review, User user);

    @Query("SELECT COALESCE(SUM(rv.vote), 0) FROM ReviewVote rv WHERE rv.review = :review")
    Long sumVotesByReview(@Param("review") Review review);
}
