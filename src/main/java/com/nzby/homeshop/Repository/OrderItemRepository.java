package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}