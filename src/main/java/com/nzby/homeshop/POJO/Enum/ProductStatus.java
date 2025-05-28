package com.nzby.homeshop.POJO.Enum;

public enum ProductStatus {
    NEW("Новинка"),
    SALE("Скидка"),
    FEATURED("Рекомендуем"),
    OUT_OF_STOCK("Нет в наличии");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
