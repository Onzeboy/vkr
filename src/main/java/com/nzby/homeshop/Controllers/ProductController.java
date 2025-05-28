package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.ProductImage;
import com.nzby.homeshop.Repository.ProductRepository;
import com.nzby.homeshop.Services.ProductService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    @Transactional
    public String showProduct(@PathVariable Long id, Model model) {
        // Загружаем продукт
        var product = productService.findProductById(id);
        if (product == null) {
            model.addAttribute("errorMessage", "Продукт не найден");
            return "product-details";
        }

        // Получаем отсортированные изображения
        List<ProductImage> sortedImages = productService.getSortedImagesForProduct(id);

        // Синхронизируем product.images с sortedImages
        product.getImages().clear();
        product.getImages().addAll(sortedImages);

        // Выбираем основное изображение
        ProductImage primaryImage = productService.getPrimaryImage(sortedImages);

        // Передаём данные в модель
        model.addAttribute("product", product);
        model.addAttribute("primaryImage", primaryImage);
        model.addAttribute("sortedImages", sortedImages);
        model.addAttribute("categoryFields", product.getCategory() != null ? product.getCategory().getProductFields() : new HashMap<>());

        return "product-details";
    }
}