package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.Enum.OrderStatus;
import com.nzby.homeshop.POJO.Order;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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


    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE o.user = :user")
    List<Order> findByUserWithItemsAndProducts(@Param("user") User user);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByCourierAndStatus(Courier courier, OrderStatus orderStatus);



    long countByStatusAndCreatedAtAfter(OrderStatus orderStatus, LocalDateTime startOfDay);

    long countByStatus(OrderStatus orderStatus);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :startOfDay AND o.courier = :courier")
    long countByStatusAndCreatedAtAfterAndCourier(@Param("status") OrderStatus status,
                                                  @Param("startOfDay") LocalDateTime startOfDay,
                                                  @Param("courier") Courier courier);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.courier = :courier")
    long countByStatusAndCourier(@Param("status") OrderStatus status, @Param("courier") Courier courier);



    @Query("SELECT o FROM Order o WHERE o.status != :completedStatus AND o.createdAt >= :startOfDay AND o.courier = :courier")
    List<Order> findActiveByCreatedAtAfterAndCourier(@Param("completedStatus") OrderStatus completedStatus,
                                                     @Param("startOfDay") LocalDateTime startOfDay,
                                                     @Param("courier") Courier courier);

    @Query("SELECT o FROM Order o WHERE o.status != :completedStatus AND o.courier = :courier")
    List<Order> findAllActiveByCourier(@Param("completedStatus") OrderStatus completedStatus,
                                       @Param("courier") Courier courier);

    @Query("SELECT o FROM Order o WHERE o.user = :user ORDER BY o.createdAt DESC, o.id DESC")
    Order findTopByUserOrderByCreatedAtDesc(User user);
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :startOfDay AND o.courier = :courier")
    List<Order> findByStatusAndCreatedAtAfterAndCourier(@Param("status") OrderStatus status,
                                                        @Param("startOfDay") LocalDateTime startOfDay,
                                                        @Param("courier") Courier courier);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.courier = :courier")
    List<Order> findByStatusAndCourier(@Param("status") OrderStatus status, @Param("courier") Courier courier);

    @Query("SELECT o FROM Order o WHERE o.status = :assignedStatus AND o.courier = :courier")
    List<Order> findAssignedByCourier(@Param("assignedStatus") OrderStatus assignedStatus,
                                      @Param("courier") Courier courier);

    @Query("SELECT o FROM Order o WHERE o.status = :assignedStatus")
    List<Order> findAllByStatus(@Param("assignedStatus") OrderStatus assignedStatus);

    @Query("SELECT o FROM Order o WHERE o.user = :user ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findByCourierAndStatusIn(Courier courier, List<OrderStatus> list);
}