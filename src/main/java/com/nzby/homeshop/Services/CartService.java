package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.CartItem;
import com.nzby.homeshop.POJO.Product;
import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

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

        // Проверка максимального количества (10 штук)
        if (quantity > 10) {
            throw new IllegalArgumentException("Максимальное количество одного товара в корзине — 10 штук.");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByUserAndProduct(user, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > 10) {
                throw new IllegalArgumentException("Максимальное количество одного товара в корзине — 10 штук.");
            }
            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(user, product, quantity);
            return cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public CartItem updateQuantity(Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Товар в корзине не найден"));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        // Проверка максимального количества (10 штук)
        if (quantity > 10) {
            throw new IllegalStateException("Максимальное количество одного товара в корзине — 10 штук.");
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    public List<CartItem> getCartItems(User user) {
        if (user == null) {
            return List.of();
        }
        return cartItemRepository.findByUser(user);
    }

    @Transactional
    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    public int getCartItemsCount(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.countByUser(user);
    }

    public List<CartItem> getCartItemsByUser(User user) {
        return cartItemRepository.findByUser(user);
    }

    // Получение товара из корзины по ID
    public CartItem getCartItemById(Long itemId) {
        Optional<CartItem> cartItem = cartItemRepository.findById(itemId);
        return cartItem.orElse(null); // Возвращаем null, если товар не найден
    }

    // Обновление товара в корзине (для совместимости с CartController)
    @Transactional
    public void updateCartItem(CartItem cartItem, Integer quantityChange) {
        int newQuantity = cartItem.getQuantity() + quantityChange;
        if (newQuantity > 10) {
            throw new IllegalStateException("Максимальное количество одного товара в корзине — 10 штук.");
        }
        if (newQuantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }
    }

    // Удаление товара из корзины
    public void removeCartItem(CartItem cartItem) {
        cartItemRepository.delete(cartItem);
    }

    public void checkout(User user) {
        List<CartItem> cartItems = getCartItemsByUser(user);

        // Преобразование и суммирование веса в кг
        BigDecimal totalWeightInKg = cartItems.stream()
                .map(item -> convertToKg(item.getProduct().getWeightValue(), item.getProduct().getWeightUnit())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Проверка лимита 20 кг
        if (totalWeightInKg.compareTo(new BigDecimal("20.0")) > 0) {
            throw new IllegalStateException("Общий вес заказа превышает 20 кг. Максимальный вес: 20 кг, текущий вес: " + totalWeightInKg + " кг");
        }

        // Оформление заказа, если вес в пределах лимита
        for (CartItem item : cartItems) {
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

    public CartItem findCartItemByUserAndProduct(User user, Product product) {
        return cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("Продукты не найдены" + product));
    }
}