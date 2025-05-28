package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.Category;
import com.nzby.homeshop.Repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    private static final List<String> AVAILABLE_FIELDS = Arrays.asList(
            "features", "description", "nutritionalInfo", "calories", "fats", "proteins", "carbohydrates", "composition"
    );

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category addCategory(String name, Long parentId, Map<String, Boolean> productFields) {
        log.info("Добавление категории: name={}, parentId={}, productFields={}",
                name, parentId, productFields);

        // Валидация
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        if (categoryRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Категория с названием '" + name + "' уже существует");
        }

        // Проверка родительской категории
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительская категория с ID " + parentId + " не найдена"));
        }

        // Фильтрация productFields
        Map<String, Boolean> filteredFields = new HashMap<>();
        for (String field : AVAILABLE_FIELDS) {
            filteredFields.put(field, productFields.getOrDefault(field, false));
        }

        // Создание категории
        Category category = new Category(name, parent, filteredFields);
        Category savedCategory = categoryRepository.save(category);
        return savedCategory;
    }

    @Transactional
    public Category updateCategory(Long id, String name, Long parentId, Map<String, Boolean> productFields) {
        log.info("Обновление категории: id={}, name={}, parentId={}, productFields={}",
                id, name, parentId, productFields);

        // Валидация
        if (id == null) {
            throw new IllegalArgumentException("ID категории не может быть пустым");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория с ID " + id + " не найдена"));

        // Проверка уникальности имени, исключая текущую категорию
        categoryRepository.findByName(name).ifPresent(existingCategory -> {
            if (!existingCategory.getId().equals(id)) {
                throw new IllegalArgumentException("Категория с названием '" + name + "' уже существует");
            }
        });

        // Проверка родительской категории
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительская категория с ID " + parentId + " не найдена"));
            // Проверяем, что категория не является своим собственным родителем
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("Категория не может быть своим собственным родителем");
            }
            // Проверяем, что родительская категория не является потомком текущей категории
            if (isDescendant(id, parentId)) {
                throw new IllegalArgumentException("Родительская категория не может быть потомком текущей категории");
            }
        }

        // Фильтрация productFields
        Map<String, Boolean> filteredFields = new HashMap<>();
        for (String field : AVAILABLE_FIELDS) {
            filteredFields.put(field, productFields.getOrDefault(field, false));
        }

        // Обновление категории
        category.setName(name.trim());
        category.setParent(parent);
        category.setProductFields(filteredFields);
        Category updatedCategory = categoryRepository.save(category);
        return updatedCategory;
    }

    private boolean isDescendant(Long categoryId, Long potentialDescendantId) {
        Category descendant = categoryRepository.findById(potentialDescendantId).orElse(null);
        while (descendant != null) {
            if (descendant.getParent() != null && descendant.getParent().getId().equals(categoryId)) {
                return true;
            }
            descendant = descendant.getParent();
        }
        return false;
    }

    public List<CategoryNode> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        List<CategoryNode> tree = new ArrayList<>();
        for (Category category : rootCategories) {
            tree.add(buildCategoryNode(category, 0));
        }
        return tree;
    }

    private CategoryNode buildCategoryNode(Category category, int level) {
        CategoryNode node = new CategoryNode(category, level);
        List<Category> subCategories = categoryRepository.findByParent(category);
        for (Category subCategory : subCategories) {
            node.getChildren().add(buildCategoryNode(subCategory, level + 1));
        }
        return node;
    }

    public boolean hasSubCategories(Long id) {
        return categoryRepository.existsByParentId(id);
    }

    public void deleteCategory(Long id) {
        if (hasSubCategories(id)) {
            throw new IllegalStateException("Нельзя удалить категорию с подкатегориями");
        }
        categoryRepository.deleteById(id);
    }
}