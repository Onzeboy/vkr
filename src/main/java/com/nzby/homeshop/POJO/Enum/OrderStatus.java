package com.nzby.homeshop.POJO.Enum;

public enum OrderStatus {
    PENDING("В обработке"),
    ASSIGNED("Назначен курьеру"),
    IN_DELIVERY("В пути"),
    DELIVERED("Доставлен"),
    CANCELLED("Отменен");

    private final String russianName;

    OrderStatus(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }

    @Override
    public String toString() {
        return russianName;
    }
}