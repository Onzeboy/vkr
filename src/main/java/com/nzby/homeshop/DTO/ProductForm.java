package com.nzby.homeshop.DTO;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductForm {
    @NotBlank(message = "Название продукта обязательно")
    @Size(max = 255, message = "Название продукта не должно превышать 255 символов")
    private String name;

    @NotBlank(message = "Бренд обязателен")
    @Size(max = 100, message = "Название бренда не должно превышать 100 символов")
    private String brand;

    @NotBlank(message = "Артикул обязателен")
    @Size(max = 50, message = "Артикул не должен превышать 50 символов")
    private String sku;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    private BigDecimal price;

    @NotNull(message = "Основная категория обязательна")
    private CategoryDTO category;

    @NotEmpty(message = "Необходимо выбрать хотя бы одну категорию")
    private List<Long> categoryIds = new ArrayList<>();

    // Опциональные поля
    @Size(max = 65535, message = "Характеристики не должны превышать 65535 символов")
    private String features;

    @Size(max = 65535, message = "Описание не должно превышать 65535 символов")
    private String description;

    @Min(value = 0, message = "Калорийность не может быть отрицательной")
    private Integer calories;

    @Min(value = 0, message = "Жиры не могут быть отрицательными")
    private Double fats;

    @Min(value = 0, message = "Белки не могут быть отрицательными")
    private Double proteins;

    @Min(value = 0, message = "Углеводы не могут быть отрицательными")
    private Double carbohydrates;

    @Size(max = 65535, message = "Состав не должен превышать 65535 символов")
    private String composition;

    // Вспомогательный DTO для категории
    public static class CategoryDTO {
        private Long id;
        private String name;
        private Map<String, Boolean> productFields;

        // Геттеры и сеттеры
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Map<String, Boolean> getProductFields() { return productFields; }
        public void setProductFields(Map<String, Boolean> productFields) { this.productFields = productFields; }
    }

    // Геттеры и сеттеры
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public CategoryDTO getCategory() { return category; }
    public void setCategory(CategoryDTO category) { this.category = category; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public Double getFats() { return fats; }
    public void setFats(Double fats) { this.fats = fats; }
    public Double getProteins() { return proteins; }
    public void setProteins(Double proteins) { this.proteins = proteins; }
    public Double getCarbohydrates() { return carbohydrates; }
    public void setCarbohydrates(Double carbohydrates) { this.carbohydrates = carbohydrates; }
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
}