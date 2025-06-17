package com.nzby.homeshop.DTO;

import com.nzby.homeshop.POJO.Enum.OrderStatus;

public class OrderStatusDTO {
    private Long id;
    private OrderStatus status;

    public OrderStatusDTO(Long id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}