package com.nzby.homeshop.POJO;

import com.nzby.homeshop.POJO.Enum.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @NotBlank(message = "Название продукта обязательно")
    @Size(max = 255, message = "Название продукта не должно превышать 255 символов")
    @Column(name = "product_name", nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Бренд обязателен")
    @Size(max = 100, message = "Название бренда не должно превышать 100 символов")
    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @NotBlank(message = "Артикул обязателен")
    @Size(max = 50, message = "Артикул не должен превышать 50 символов")
    @Column(name = "sku", nullable = false, length = 50, unique = true)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Вес не может быть отрицательным")
    @Column(name = "weight_value")
    private Double weightValue;

    @Column(name = "weight_unit", length = 10)
    private String weightUnit;

    @Size(max = 65535, message = "Характеристика не должно превышать 65535 символов")
    @Column(name = "features", columnDefinition = "TEXT")
    private String features;

    @Size(max = 65535, message = "Описание продукта не должно превышать 65535 символов")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Min(value = 0, message = "Калорийность не может быть отрицательной")
    @Column(name = "calories")
    private Integer calories;

    @Min(value = 0, message = "Жиры не могут быть отрицательными")
    @Column(name = "fats")
    private Double fats;

    @Min(value = 0, message = "Белки не могут быть отрицательными")
    @Column(name = "proteins")
    private Double proteins;

    @Min(value = 0, message = "Углеводы не могут быть отрицательными")
    @Column(name = "carbohydrates")
    private Double carbohydrates;

    @Size(max = 65535, message = "Состав не должен превышать 65535 символов")
    @Column(name = "composition", columnDefinition = "TEXT")
    private String composition;

    @DecimalMin(value = "0.0", message = "Средняя оценка не может быть отрицательной")
    @DecimalMax(value = "5.0", message = "Средняя оценка не может превышать 5.0")
    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Min(value = 0, message = "Продажи за месяц не могут быть отрицательными")
    @Column(name = "monthly_sales")
    private Integer monthlySales;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Min(value = 0, message = "Количество оценок не может быть отрицательным")
    @Column(name = "ratings_count")
    private Integer ratingsCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_ordered")
    private LocalDateTime lastOrdered;

    @Column(name = "cluster_id")
    private Integer clusterId;

    @Min(value = 0, message = "Общие продажи не могут быть отрицательными")
    @Column(name = "total_sales")
    private Integer totalSales;

    @Min(value = 0, message = "Запас не может быть отрицательным")
    @Column(name = "stock")
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ProductStatus status;

    @NotEmpty(message = "Необходимо выбрать хотя бы одну категорию")
    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @DecimalMin(value = "0.0", message = "Цена со скидкой не может быть отрицательной")
    @Column(name = "discounted_price")
    private BigDecimal discountedPrice;

    @Column(name = "discount_start_date")
    private LocalDateTime discountStartDate;

    @Column(name = "discount_end_date")
    private LocalDateTime discountEndDate;


    // Конструкторы
    public Product() {
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getWeightValue() {
        return weightValue;
    }

    public void setWeightValue(Double weightValue) {
        this.weightValue = weightValue;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Double getFats() {
        return fats;
    }

    public void setFats(Double fats) {
        this.fats = fats;
    }

    public Double getProteins() {
        return proteins;
    }

    public void setProteins(Double proteins) {
        this.proteins = proteins;
    }

    public Double getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(Double carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public String getComposition() {
        return composition;
    }

    public void setComposition(String composition) {
        this.composition = composition;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(Integer monthlySales) {
        this.monthlySales = monthlySales;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Integer getRatingsCount() {
        return ratingsCount;
    }

    public void setRatingsCount(Integer ratingsCount) {
        this.ratingsCount = ratingsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastOrdered() {
        return lastOrdered;
    }

    public void setLastOrdered(LocalDateTime lastOrdered) {
        this.lastOrdered = lastOrdered;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Integer totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public void removeCategory(Category category) {
        this.categories.remove(category);
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public LocalDateTime getDiscountStartDate() {
        return discountStartDate;
    }

    public void setDiscountStartDate(LocalDateTime discountStartDate) {
        this.discountStartDate = discountStartDate;
    }

    public LocalDateTime getDiscountEndDate() {
        return discountEndDate;
    }

    public void setDiscountEndDate(LocalDateTime discountEndDate) {
        this.discountEndDate = discountEndDate;
    }

    public int getDiscountPercentage() {
        if (price == null || discountedPrice == null || price.compareTo(BigDecimal.ZERO) <= 0 || discountedPrice.compareTo(price) >= 0) {
            return 0;
        }
        try {
            BigDecimal discount = price.subtract(discountedPrice)
                    .divide(price, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP);
            return discount.intValue();
        } catch (Exception e) {
            // Логирование ошибки, если нужно
            return 0;
        }
    }
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", sku='" + sku + '\'' +
                ", category=" + category +
                ", categories=" + categories +
                ", price=" + price +
                ", weightValue=" + weightValue +
                ", weightUnit='" + weightUnit + '\'' +
                ", features='" + features + '\'' +
                '}';
    }
}