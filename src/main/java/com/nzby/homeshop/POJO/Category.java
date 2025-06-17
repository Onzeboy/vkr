package com.nzby.homeshop.POJO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_name", columnList = "category_name"),
        @Index(name = "idx_categories_parent_id", columnList = "parent_id")
})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название категории обязательно")
    @Size(max = 255, message = "Название категории не должно превышать 255 символов")
    @Column(name = "category_name", nullable = false)
    private String name;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "product_fields", columnDefinition = "TEXT")
    private String productFields;

    // Конструкторы
    public Category() {
    }

    public Category(String name, Category parent, Map<String, Boolean> productFields) {
        this.name = name;
        this.parent = parent;
        setProductFields(productFields);
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

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public Map<String, Boolean> getProductFields() {
        if (productFields == null) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(productFields, new TypeReference<Map<String, Boolean>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при парсинге productFields из JSON", e);
        }
    }

    public void setProductFields(Map<String, Boolean> fields) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.productFields = mapper.writeValueAsString(fields);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сериализации productFields в JSON", e);
        }
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parent=" + (parent != null ? parent.getName() : null) +
                '}';
    }
}