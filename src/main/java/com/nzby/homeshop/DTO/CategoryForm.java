package com.nzby.homeshop.DTO;

import java.util.Map;

public class CategoryForm {
    private Long id;
    private String name;
    private Map<String, Boolean> productFields;
    private Long parentId;  // или другой тип, который вы используете

    // Constructors
    public CategoryForm() {}

    public CategoryForm(Long id, String name, Map<String, Boolean> productFields) {
        this.id = id;
        this.name = name;
        this.productFields = productFields;
    }
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    // Getters and Setters
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

    public Map<String, Boolean> getProductFields() {
        return productFields;
    }

    public void setProductFields(Map<String, Boolean> productFields) {
        this.productFields = productFields;
    }
}