package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryNode {
    private Category category;
    private int level;
    private List<CategoryNode> children;

    public CategoryNode(Category category, int level) {
        this.category = category;
        this.level = level;
        this.children = new ArrayList<>();
    }

    public Category getCategory() {
        return category;
    }

    public int getLevel() {
        return level;
    }

    public List<CategoryNode> getChildren() {
        return children;
    }
}