package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.DTO.CategoryForm;
import com.nzby.homeshop.POJO.Category;
import com.nzby.homeshop.Repository.CategoryRepository;
import com.nzby.homeshop.Services.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    // Определите allowedFields с features
    private static final List<String> allowedFields = Arrays.asList("features", "description", "nutritionalInfo", "composition", "calories", "fats", "proteins", "carbohydrates");

    @Autowired
    public CategoryController(CategoryRepository categoryRepository, CategoryService categoryService) {
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categoryTree", categoryService.getCategoryTree());
        return "admin/category-list";
    }

    @GetMapping("/add")
    public String showAddCategoryForm(Model model) {
        CategoryForm form = new CategoryForm();
        model.addAttribute("categoryForm", form);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "admin/category-add";
    }

    @PostMapping("/add")
    public String addCategory(@Valid CategoryForm categoryForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allCategories", categoryRepository.findAll());
            return "admin/category-add";
        }

        String name = categoryForm.getName();
        if (name == null || name.trim().isEmpty()) {
            bindingResult.rejectValue("name", "error.name", "Название категории не может быть пустым");
            model.addAttribute("allCategories", categoryRepository.findAll());
            return "admin/category-add";
        }

        name = name.trim();

        if (categoryRepository.findByName(name).isPresent()) {
            bindingResult.rejectValue("name", "error.name", "Категория с таким названием уже существует");
            model.addAttribute("allCategories", categoryRepository.findAll());
            return "admin/category-add";
        }

        Category category = new Category();
        category.setName(name);

        if (categoryForm.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryForm.getParentId()).orElse(null);
            category.setParent(parent);
        }

        Map<String, Boolean> filteredFields = new HashMap<>();
        for (String field : allowedFields) {
            Boolean value = categoryForm.getProductFields().get(field);
            filteredFields.put(field, value != null ? value : false);
        }
        if (filteredFields.get("nutritionalInfo")) {
            filteredFields.put("calories", true);
            filteredFields.put("fats", true);
            filteredFields.put("proteins", true);
            filteredFields.put("carbohydrates", true);
        }
        category.setProductFields(filteredFields);

        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Категория с ID " + id + " не найдена");
                    return null;
                });

        if (category == null) {
            return "redirect:/admin/categories";
        }

        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        form.setProductFields(category.getProductFields() != null ? new HashMap<>(category.getProductFields()) : new HashMap<>());

        model.addAttribute("categoryForm", form);
        model.addAttribute("allCategories", categoryRepository.findAll());
        return "admin/category-edit";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id, @Valid CategoryForm categoryForm, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        logger.info("Полученные данные: {}", categoryForm.getProductFields());
        if (bindingResult.hasErrors()) {
            return "admin/category-edit";
        }

        String name = categoryForm.getName();
        if (name == null || name.trim().isEmpty()) {
            bindingResult.rejectValue("name", "error.name", "Название категории не может быть пустым");
            return "admin/category-edit";
        }

        name = name.trim();

        categoryRepository.findByName(name).ifPresent(existingCategory -> {
            if (!existingCategory.getId().equals(id)) {
                bindingResult.rejectValue("name", "error.name", "Категория с таким названием уже существует");
            }
        });

        if (bindingResult.hasErrors()) {
            return "admin/category-edit";
        }

        try {
            Map<String, Boolean> filteredFields = new HashMap<>();
            for (String field : allowedFields) {
                filteredFields.put(field, false); // Инициализация всех полей
            }
            for (String field : allowedFields) {
                Boolean value = categoryForm.getProductFields().get(field);
                filteredFields.put(field, value != null && value); // Устанавливаем true только если значение есть и равно true
                if (field.equals("nutritionalInfo") && filteredFields.get(field)) {
                    filteredFields.put("calories", true);
                    filteredFields.put("fats", true);
                    filteredFields.put("proteins", true);
                    filteredFields.put("carbohydrates", true);
                }
            }

            logger.info("Обновленные productFields: {}", filteredFields);
            categoryService.updateCategory(id, name, categoryForm.getParentId(), filteredFields);
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories/edit/" + id;
        }
    }
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
        } catch (DataIntegrityViolationException e) {
            logger.error("Ошибка при удалении категории с ID {}: {}", id, e.getMessage());
            String errorMessage = "Невозможно удалить категорию, так как она используется в продуктах.";
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } catch (IllegalStateException e) {
            logger.error("Ошибка состояния при удалении категории с ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}