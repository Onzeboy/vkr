package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    List<Order> findByUser(User user);

    // Первый запрос: загружаем Order с OrderItems и Product
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.product")
    List<Order> findAllWithItemsAndProducts();

    // Второй запрос: загружаем Product с Images
    @Query("SELECT p FROM Product p JOIN FETCH p.images WHERE p IN :products")
    List<Product> findProductsWithImages(List<Product> products);

    List<Order> findByUserOrderByOrderDateDesc(User user);

    Optional<Order> findByIdAndUser(Long id, User user);
}