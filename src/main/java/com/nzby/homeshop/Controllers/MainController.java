package com.nzby.homeshop.Controllers;

import com.nzby.homeshop.Services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String getMainPage(Model model) {
        model.addAttribute("allProducts", productService.getRandomProducts(5));
        return "mainpage";
    }

    @GetMapping("/products")
    public String getAllProducts(Model model) {
        model.addAttribute("allProducts", productService.getAllProducts());
        return "all";
    }
}