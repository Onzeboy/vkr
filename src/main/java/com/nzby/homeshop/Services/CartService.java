package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.Repository.CartItemRepository;
import com.nzby.homeshop.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public CartItem addToCart(User user, Product product, int quantity) {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        if (product == null) {
            throw new IllegalArgumentException("Товар не может быть null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        if (quantity > 10) {
            throw new IllegalArgumentException("Максимальное количество одного товара в корзине — 10 штук.");
        }

        if (quantity > product.getStock()) {
            throw new IllegalArgumentException("На складе недостаточно товара. Доступно: " + product.getStock());
        }

        Optional<CartItem> existingItem = cartItemRepository.findByUserAndProduct(user, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > 10) {
                throw new IllegalArgumentException("Максимальное количество одного товара в корзине — 10 штук.");
            }
            item.setQuantity(newQuantity);
            item.setUpdatedAt(LocalDateTime.now());
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(user, product, quantity);
            newItem.setUnitPrice(product.getPrice());
            newItem.setAddedAt(LocalDateTime.now());
            newItem.setOrdered(false);
            return cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public void updateCartItem(CartItem cartItem, Integer quantityChange) {
        int newQuantity = cartItem.getQuantity() + quantityChange;
        Product product = cartItem.getProduct();

        if (newQuantity < 0) {
            throw new IllegalStateException("Количество не может быть отрицательным");
        }
        if (newQuantity == 0) {
            cartItemRepository.delete(cartItem);
            updateProductStock(product, product.getStock() - quantityChange);
            return;
        }
        if (newQuantity > 10) {
            throw new IllegalStateException("Максимальное количество одного товара в корзине — 10 штук.");
        }

        if (newQuantity > product.getStock()) {
            throw new IllegalStateException("На складе недостаточно товара. Доступно: " + product.getStock());
        }

        cartItem.setQuantity(newQuantity);
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartItemRepository.save(cartItem);
        updateProductStock(product, product.getStock() - quantityChange);
    }

    @Transactional
    private void updateProductStock(Product product, int newStock) {
        product.setStock(newStock);
        if (newStock <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            // Reset status to a default if stock is replenished
            product.setStatus(ProductStatus.NEW);
        }
        productRepository.save(product);
    }

    public List<CartItem> getCartItems(User user) {
        if (user == null) {
            return List.of();
        }
        return cartItemRepository.findByUser(user);
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Элемент корзины не найден"));
        Product product = cartItem.getProduct();
        int quantity = cartItem.getQuantity();
        cartItemRepository.deleteById(cartItemId);
        updateProductStock(product, product.getStock() + quantity);
    }

    public int getCartItemsCount(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.countByUser(user);
    }

    public List<CartItem> getCartItemsByUser(User user) {
        return cartItemRepository.findByUserAndIsOrdered(user, false);
    }

    public CartItem getCartItemById(Long itemId) {
        return cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Элемент корзины не найден"));
    }

    @Transactional
    public void removeCartItem(CartItem cartItem) {
        Product product = cartItem.getProduct();
        int quantity = cartItem.getQuantity();
        cartItemRepository.delete(cartItem);
        updateProductStock(product, product.getStock() + quantity);
    }

    @Transactional
    public void checkout(User user, List<Long> selectedItemIds) {
        List<CartItem> cartItems = getCartItemsByUser(user);
        List<CartItem> selectedItems = cartItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .collect(Collectors.toList());

        BigDecimal totalWeightInKg = selectedItems.stream()
                .map(item -> convertToKg(item.getProduct().getWeightValue(), item.getProduct().getWeightUnit())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeightInKg.compareTo(new BigDecimal("10.0")) > 0) {
            throw new IllegalStateException("Общий вес заказа превышает 10 кг. Максимальный вес: 10 кг, текущий вес: " + totalWeightInKg + " кг");
        }

        for (CartItem item : selectedItems) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            updateProductStock(product, newStock);
            item.setOrdered(true);
            cartItemRepository.save(item);
        }
    }

    public BigDecimal convertToKg(Double weightValue, String weightUnit) {
        if (weightValue == null || weightUnit == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = BigDecimal.valueOf(weightValue);
        switch (weightUnit.toLowerCase()) {
            case "кг":
                return value;
            case "г":
                return value.divide(BigDecimal.valueOf(1000), 3, BigDecimal.ROUND_HALF_UP);
            case "мл":
                return value.divide(BigDecimal.valueOf(1000), 3, BigDecimal.ROUND_HALF_UP);
            case "л":
                return value;
            default:
                throw new IllegalArgumentException("Неизвестная единица измерения: " + weightUnit);
        }
    }

    public BigDecimal calculateSelectedTotalPrice(List<CartItem> cartItems, List<Long> selectedItemIds) {
        return cartItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateSelectedTotalWeight(List<CartItem> cartItems, List<Long> selectedItemIds) {
        return cartItems.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .map(item -> convertToKg(item.getProduct().getWeightValue(), item.getProduct().getWeightUnit())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isWeightLimitExceeded(List<CartItem> cartItems, List<Long> selectedItemIds) {
        BigDecimal totalWeight = calculateSelectedTotalWeight(cartItems, selectedItemIds);
        return totalWeight.compareTo(BigDecimal.valueOf(10)) > 0;
    }

    public CartItem findCartItemByUserAndProduct(User user, Product product) {
        return cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Продукт не найден: " + product));
    }

    public int getProductStock(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getStock)
                .orElse(0);
    }

    public void updateCartItemSelection(CartItem cartItem, Boolean isSelected) {
        cartItem.setOrdered(isSelected);
        cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItemsByUserAndOrdered(User user, boolean isOrdered) {
        return cartItemRepository.findByUserAndIsOrdered(user, isOrdered);
    }
}