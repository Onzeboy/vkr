package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.POJO.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserEmail(String email);
    Optional<Wishlist> findByUserEmailAndProductId(String email, Long productId);

    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.email = :email")
    List<Long> findProductIdsByUserEmail(String email);

    List<Wishlist> findByUser(User user);
}