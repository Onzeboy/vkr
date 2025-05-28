package com.nzby.homeshop.Controllers.Admin;

import com.nzby.homeshop.DTO.CategoryForm;
import com.nzby.homeshop.POJO.Category;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.ProductImage;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.Services.ProductService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
public class ProductCRUDController {

    private static final Logger log = LoggerFactory.getLogger(ProductCRUDController.class);

    @Autowired
    private ProductService productService;

    @GetMapping
    public String showProductList(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search);
        } else {
            products = productService.findAll();
        }
        model.addAttribute("products", products);
        model.addAttribute("search", search);
        return "admin/product-list";
    }

    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("rootCategories", productService.getRootCategories());
        model.addAttribute("allCategories", productService.getAllCategories());
        return "admin/addprod";
    }

    @PostMapping("/add")
    @Transactional
    public String addProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "photos[]", required = false) MultipartFile[] photos,
            @RequestParam(value = "primaryImage", required = false) String primaryImage,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "subcategoryId", required = false) Long subcategoryId,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "calories", required = false) Integer calories,
            @RequestParam(value = "fats", required = false) Double fats,
            @RequestParam(value = "proteins", required = false) Double proteins,
            @RequestParam(value = "carbohydrates", required = false) Double carbohydrates,
            @RequestParam(value = "composition", required = false) String composition,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Attempting to add product: {}", product.getName());
        log.info("Received {} photos", photos != null ? photos.length : 0);
        log.info("Category ID: {}, Subcategory ID: {}", categoryId, subcategoryId);

        Long selectedCategoryId = subcategoryId != null ? subcategoryId : categoryId;
        if (selectedCategoryId == null) {
            result.rejectValue("category", "category.required", "Выберите категорию или подкатегорию");
        } else {
            Category category = productService.findCategoryById(selectedCategoryId);
            if (category == null) {
                result.rejectValue("category", "category.invalid", "Недопустимая категория");
            } else {
                product.setCategory(category);
                product.getCategories().clear();
                product.getCategories().add(category);
            }
        }

        if (result.hasErrors()) {
            log.warn("Validation errors found for product: {}", product.getName());
            model.addAttribute("rootCategories", productService.getRootCategories());
            model.addAttribute("allCategories", productService.getAllCategories());
            return "admin/addprod";
        }

        product.setStatus(ProductStatus.NEW);
        product.setCreatedAt(LocalDateTime.now());
        if (product.getAverageRating() == null) {
            product.setAverageRating(BigDecimal.ZERO);
        }

        productService.setupTags(product, tags);
        productService.setupNutritionalInfo(product, calories, fats, proteins, carbohydrates, composition);

        if (selectedCategoryId != null) {
            Category category = productService.findCategoryById(selectedCategoryId);
            Map<String, Boolean> fields = category.getProductFields();
            if (!fields.getOrDefault("features", false)) {
                product.setFeatures(null);
            }
            if (!fields.getOrDefault("description", false)) {
                product.setDescription(null);
            }
            if (!fields.getOrDefault("nutritionalInfo", false)) {
                product.setCalories(null);
                product.setFats(null);
                product.setProteins(null);
                product.setCarbohydrates(null);
            }
            if (!fields.getOrDefault("composition", false)) {
                product.setComposition(null);
            }
        }

        try {
            productService.saveProduct(product);
            log.info("Product saved with ID: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to save product: {}", e.getMessage(), e);
            result.reject("product.error", "Ошибка при сохранении продукта: " + e.getMessage());
            model.addAttribute("rootCategories", productService.getRootCategories());
            model.addAttribute("allCategories", productService.getAllCategories());
            return "admin/addprod";
        }

        List<ProductImage> productImages = null;
        if (photos != null && photos.length > 0 && !(photos.length == 1 && photos[0].isEmpty())) {
            try {
                productImages = productService.processAndSavePhotos(product, photos, result, primaryImage);
                if (result.hasErrors()) {
                    log.warn("Errors found during photo processing for product: {}", product.getName());
                    productService.deleteProduct(product.getId());
                    model.addAttribute("rootCategories", productService.getRootCategories());
                    model.addAttribute("allCategories", productService.getAllCategories());
                    return "admin/addprod";
                }
            } catch (Exception e) {
                log.error("Exception during photo processing for product {}: {}", product.getName(), e.getMessage());
                result.reject("images.error", "Ошибка при обработке фотографий: " + e.getMessage());
                productService.deleteProduct(product.getId());
                model.addAttribute("rootCategories", productService.getRootCategories());
                model.addAttribute("allCategories", productService.getAllCategories());
                return "admin/addprod";
            }

            try {
                log.info("Clearing existing images for product ID: {}", product.getId());
                product.getImages().clear();
                log.info("Adding {} new images for product ID: {}", productImages.size(), product.getId());
                product.getImages().addAll(productImages);
                productService.saveProduct(product);
                log.info("Product with images saved successfully: {}", product.getName());
            } catch (Exception e) {
                log.error("Failed to save product with images: {}", e.getMessage(), e);
                result.reject("images.error", "Ошибка при сохранении изображений: " + e.getMessage());
                productService.deleteProduct(product.getId());
                model.addAttribute("rootCategories", productService.getRootCategories());
                model.addAttribute("allCategories", productService.getAllCategories());
                return "admin/addprod";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Продукт успешно добавлен!");
        return "redirect:/products/" + product.getId(); // Редирект на страницу продукта
    }

    @GetMapping("/edit")
    public String showEditProductForm(@RequestParam("id") Long id, Model model) {
        log.info("Fetching product for editing with ID: {}", id);
        Product product = productService.findProductById(id);
        if (product == null) {
            log.warn("Product with ID {} not found", id);
            model.addAttribute("error", "Продукт не найден");
            return "admin/product-list";
        }
        model.addAttribute("product", product);
        model.addAttribute("rootCategories", productService.getRootCategories());
        model.addAttribute("allCategories", productService.getAllCategories());

        Long categoryId = product.getCategory() != null ?
                (product.getCategory().getParent() != null ? product.getCategory().getParent().getId() : product.getCategory().getId()) : null;
        Long subcategoryId = product.getCategory() != null && product.getCategory().getParent() != null ?
                product.getCategory().getId() : null;

        if (categoryId != null) {
            List<CategoryForm> subcategories = productService.getSubcategories(categoryId);
            model.addAttribute("subcategories", subcategories);

            Category category = productService.findCategoryById(subcategoryId != null ? subcategoryId : categoryId);
            if (category != null) {
                model.addAttribute("categoryFields", category.getProductFields());
            }
        }

        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSubcategoryId", subcategoryId);
        return "admin/editprod";
    }

    @PostMapping("/edit")
    @Transactional
    public String editProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "photos[]", required = false) MultipartFile[] photos,
            @RequestParam(value = "primaryImage", required = false) String primaryImage,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "subcategoryId", required = false) Long subcategoryId,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "calories", required = false) Integer calories,
            @RequestParam(value = "fats", required = false) Double fats,
            @RequestParam(value = "proteins", required = false) Double proteins,
            @RequestParam(value = "carbohydrates", required = false) Double carbohydrates,
            @RequestParam(value = "composition", required = false) String composition,
            @RequestParam(value = "deleteImages", required = false) List<Long> deleteImages,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Attempting to update product ID: {}", product.getId());
        log.info("Received {} photos", photos != null ? photos.length : 0);
        log.info("Category ID: {}, Subcategory ID: {}", categoryId, subcategoryId);

        Product existingProduct = productService.findProductById(product.getId());
        if (existingProduct == null) {
            log.warn("Product with ID {} not found", product.getId());
            result.reject("product.error", "Продукт не найден");
            model.addAttribute("rootCategories", productService.getRootCategories());
            model.addAttribute("allCategories", productService.getAllCategories());
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedSubcategoryId", subcategoryId);
            return "admin/editprod";
        }

        Long selectedCategoryId = subcategoryId != null ? subcategoryId : categoryId;
        if (selectedCategoryId == null) {
            result.rejectValue("category", "category.required", "Выберите категорию или подкатегорию");
        } else {
            Category category = productService.findCategoryById(selectedCategoryId);
            if (category == null) {
                result.rejectValue("category", "category.invalid", "Недопустимая категория");
            } else {
                product.setCategory(category);
                product.getCategories().clear();
                product.getCategories().add(category);
            }
        }

        if (result.hasErrors()) {
            log.warn("Validation errors found for product ID: {}", product.getId());
            model.addAttribute("rootCategories", productService.getRootCategories());
            model.addAttribute("allCategories", productService.getAllCategories());
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedSubcategoryId", subcategoryId);
            return "admin/editprod";
        }

        product.setCreatedAt(existingProduct.getCreatedAt());
        if (product.getAverageRating() == null) {
            product.setAverageRating(existingProduct.getAverageRating());
        }

        productService.setupTags(product, tags);
        productService.setupNutritionalInfo(product, calories, fats, proteins, carbohydrates, composition);

        if (selectedCategoryId != null) {
            Category category = productService.findCategoryById(selectedCategoryId);
            Map<String, Boolean> fields = category.getProductFields();
            if (!fields.getOrDefault("features", false)) {
                product.setFeatures(null);
            }
            if (!fields.getOrDefault("description", false)) {
                product.setDescription(null);
            }
            if (!fields.getOrDefault("nutritionalInfo", false)) {
                product.setCalories(null);
                product.setFats(null);
                product.setProteins(null);
                product.setCarbohydrates(null);
            }
            if (!fields.getOrDefault("composition", false)) {
                product.setComposition(null);
            }
        }

        if (deleteImages != null && !deleteImages.isEmpty()) {
            try {
                productService.deleteImages(deleteImages);
                product.getImages().removeIf(image -> deleteImages.contains(image.getId()));
                log.info("Deleted {} images for product ID: {}", deleteImages.size(), product.getId());
            } catch (Exception e) {
                log.error("Failed to delete images: {}", e.getMessage(), e);
                result.reject("images.error", "Ошибка при удалении изображений: " + e.getMessage());
                model.addAttribute("rootCategories", productService.getRootCategories());
                model.addAttribute("allCategories", productService.getAllCategories());
                model.addAttribute("selectedCategoryId", categoryId);
                model.addAttribute("selectedSubcategoryId", subcategoryId);
                return "admin/editprod";
            }
        }

        try {
            productService.saveProduct(product);
            log.info("Product updated with ID: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to update product: {}", e.getMessage(), e);
            result.reject("product.error", "Ошибка при обновлении продукта: " + e.getMessage());
            model.addAttribute("rootCategories", productService.getRootCategories());
            model.addAttribute("allCategories", productService.getAllCategories());
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedSubcategoryId", subcategoryId);
            return "admin/editprod";
        }

        List<ProductImage> productImages = null;
        if (photos != null && photos.length > 0 && !(photos.length == 1 && photos[0].isEmpty())) {
            try {
                productImages = productService.processAndSavePhotos(product, photos, result, primaryImage);
                if (result.hasErrors()) {
                    log.warn("Errors found during photo processing for product ID: {}", product.getId());
                    model.addAttribute("rootCategories", productService.getRootCategories());
                    model.addAttribute("allCategories", productService.getAllCategories());
                    model.addAttribute("selectedCategoryId", categoryId);
                    model.addAttribute("selectedSubcategoryId", subcategoryId);
                    return "admin/editprod";
                }
            } catch (Exception e) {
                log.error("Exception during photo processing for product ID {}: {}", product.getId(), e.getMessage());
                result.reject("images.error", "Ошибка при обработке фотографий: " + e.getMessage());
                model.addAttribute("rootCategories", productService.getRootCategories());
                model.addAttribute("allCategories", productService.getAllCategories());
                model.addAttribute("selectedCategoryId", categoryId);
                model.addAttribute("selectedSubcategoryId", subcategoryId);
                return "admin/editprod";
            }

            try {
                product.getImages().addAll(productImages);
                productService.saveProduct(product);
                log.info("Product with new images saved successfully: {}", product.getName());
            } catch (Exception e) {
                log.error("Failed to save product with new images: {}", e.getMessage(), e);
                result.reject("images.error", "Ошибка при сохранении изображений: " + e.getMessage());
                model.addAttribute("rootCategories", productService.getRootCategories());
                model.addAttribute("allCategories", productService.getAllCategories());
                model.addAttribute("selectedCategoryId", categoryId);
                model.addAttribute("selectedSubcategoryId", subcategoryId);
                return "admin/editprod";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Продукт успешно обновлен!");
        return "redirect:/products/" + product.getId(); // Редирект на страницу продукта
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Attempting to delete product with ID: {}", id);
        try {
            Product product = productService.findProductById(id);
            if (product == null) {
                log.warn("Product with ID {} not found", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Продукт не найден");
            } else {
                productService.deleteProduct(id);
                log.info("Product with ID {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("successMessage", "Продукт успешно удалён");
            }
        } catch (Exception e) {
            log.error("Failed to delete product with ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении продукта: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/subcategories")
    @ResponseBody
    public List<CategoryForm> getSubcategories(@RequestParam("categoryId") Long categoryId) {
        log.info("Fetching subcategories for category ID: {}", categoryId);
        return productService.getSubcategories(categoryId);
    }

    @GetMapping("/api/categories/{categoryId}/fields")
    @ResponseBody
    public Map<String, Boolean> getCategoryFields(@PathVariable("categoryId") Long categoryId) {
        log.info("Fetching fields for category ID: {}", categoryId);
        Category category = productService.findCategoryById(categoryId);
        if (category == null) {
            log.warn("Category with ID {} not found", categoryId);
            throw new IllegalArgumentException("Category not found: " + categoryId);
        }
        return category.getProductFields();
    }
}