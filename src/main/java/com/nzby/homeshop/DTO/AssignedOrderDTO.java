package com.nzby.homeshop.DTO;

import com.nzby.homeshop.POJO.Enum.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AssignedOrderDTO {
    private Long id;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal totalWeightInGrams;

    public AssignedOrderDTO() {}

    public AssignedOrderDTO(Long id, LocalDateTime orderDate, OrderStatus status, BigDecimal totalAmount, BigDecimal totalWeightInGrams) {
        this.id = id;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.totalWeightInGrams = totalWeightInGrams;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalWeightInGrams() {
        return totalWeightInGrams;
    }

    public void setTotalWeightInGrams(BigDecimal totalWeightInGrams) {
        this.totalWeightInGrams = totalWeightInGrams;
    }
}