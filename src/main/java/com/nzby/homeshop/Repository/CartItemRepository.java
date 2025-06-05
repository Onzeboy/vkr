package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    List<CartItem> findByUser(User user);
    int countByUser(User user);

    @Query("SELECT c FROM CartItem c JOIN FETCH c.product WHERE c.user = :user AND c.isOrdered = :isOrdered")
    List<CartItem> findByUserAndIsOrdered(@Param("user") User user, @Param("isOrdered") Boolean isOrdered);
}