package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.Repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger Logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Transactional
    public Order createOrder(User user, List<Long> selectedItemIds, Address address, String paymentMethod) {
        if (user == null) {
            throw new IllegalStateException("Пользователь не найден");
        }

        List<CartItem> selectedCartItems = cartItemRepository.findByUserAndIsOrdered(user, true);

        if (selectedCartItems.isEmpty()) {
            throw new IllegalStateException("Не выбрано ни одного товара для заказа");
        }


        if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
            selectedCartItems = selectedCartItems.stream()
                    .filter(cartItem -> selectedItemIds.contains(cartItem.getId()))
                    .collect(Collectors.toList());
            if (selectedCartItems.isEmpty()) {
                throw new IllegalStateException("Выбранные товары не найдены в корзине или не отмечены для заказа");
            }
        }

        // Проверяем остатки товаров
        for (var cartItem : selectedCartItems) {
            var product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Недостаточно товара '" +
                        product.getId() + "' на складе. id: " + cartItem.getId());
            }
        }

        // Рассчитываем общую стоимость с учетом всех товаров
        BigDecimal totalPrice = selectedCartItems.stream()
                .map(cartItem -> {
                    BigDecimal unitPrice = cartItem.getProduct().getDiscountedPrice() != null &&
                            cartItem.getProduct().getDiscountedPrice().compareTo(BigDecimal.ZERO) > 0
                            ? cartItem.getProduct().getDiscountedPrice()
                            : cartItem.getProduct().getPrice();
                    return unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Рассчитываем вес
        BigDecimal totalWeightInKg = selectedCartItems.stream()
                .map(cartItem -> {
                    BigDecimal weightInGrams = cartItem.getProduct().getWeightValue() != null
                            ? BigDecimal.valueOf(cartItem.getProduct().getWeightValue())
                            : BigDecimal.ZERO;
                    return weightInGrams.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        if (totalWeightInKg.compareTo(BigDecimal.valueOf(10)) > 0) {
            throw new IllegalStateException("Общий вес заказа превышает 10 кг. Максимальный вес: 10 кг, текущий вес: " + totalWeightInKg + " кг");
        }

        // Проверяем минимальную сумму заказа
        if (totalPrice.compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalStateException("Минимальная сумма заказа должна быть не менее 1000 рублей. Текущая сумма: " + totalPrice + " рублей");
        }

        // Проверяем и сохраняем адрес
        Address existingAddress = addressRepository.findByUserAndStreetAddressAndCity(
                user, address.getStreetAddress(), address.getCity());
        if (existingAddress == null) {
            address.setUser(user);
            existingAddress = addressRepository.save(address);
        }

        // Создаём заказ
        Order order = new Order();
        order.setUser(user);
        order.setAddress(existingAddress);
        order.setTotalAmount(totalPrice);
        order.setTotalWeightInGrams(totalWeightInKg.multiply(BigDecimal.valueOf(1000)));
        order.setPaymentMethod(paymentMethod);
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());

        // Преобразуем CartItem в OrderItem без привязки к order
        List<OrderItem> orderItems = selectedCartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            BigDecimal unitPrice = cartItem.getProduct().getDiscountedPrice() != null &&
                    cartItem.getProduct().getDiscountedPrice().compareTo(BigDecimal.ZERO) > 0
                    ? cartItem.getProduct().getDiscountedPrice()
                    : cartItem.getProduct().getPrice();
            orderItem.setPrice(unitPrice);
            return orderItem;
        }).collect(Collectors.toList());

        // Добавляем элементы заказа через addOrderItem
        orderItems.forEach(order::addOrderItem);

        // Сохраняем заказ
        order = orderRepository.save(order);

        // Обновляем остатки товаров и удаляем элементы корзины
        for (var cartItem : selectedCartItems) {
            var product = cartItem.getProduct();
            int newStock = product.getStock() - cartItem.getQuantity();
            product.setStock(newStock);
            if (newStock <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.NEW);
            }
            productRepository.save(product);
            cartItemRepository.delete(cartItem);
        }

        return order;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    @Transactional
    public List<Order> getOrdersByUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Пользователь не указан");
        }
        return orderRepository.findByUser(user);
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> getOrdersWithImages(User user) {
        List<Order> orders = orderRepository.findAllWithItemsAndProducts();
        List<Product> products = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getProduct)
                .distinct()
                .collect(Collectors.toList());

        if (!products.isEmpty()) {
            orderRepository.findProductsWithImages(products);
        }

        orders.forEach(order -> {
            Logger.info("Заказ: {}, Элементы заказа: {}, Продукт: {}, Изображения: {}",
                    order,
                    order.getOrderItems(),
                    order.getOrderItems() != null && !order.getOrderItems().isEmpty() ? order.getOrderItems().get(0).getProduct() : "Нет продукта",
                    order.getOrderItems() != null && !order.getOrderItems().isEmpty() && order.getOrderItems().get(0).getProduct() != null
                            ? order.getOrderItems().get(0).getProduct().getImages() : "Нет изображений");
        });

        return orders;
    }
    @Transactional
    public List<Address> getLastThreeUsedAddresses(User user) {
        if (user == null) {
            throw new IllegalStateException("Пользователь не найден");
        }

        // Находим все заказы пользователя, отсортированные по дате заказа (от новых к старым)
        List<Order> userOrders = orderRepository.findByUserOrderByOrderDateDesc(user);

        // Извлекаем уникальные адреса из заказов
        Set<Address> uniqueAddresses = new LinkedHashSet<>();
        for (Order order : userOrders) {
            if (order.getAddress() != null) {
                uniqueAddresses.add(order.getAddress());
                if (uniqueAddresses.size() >= 3) {
                    break; // Прерываем, если собрали 3 уникальных адреса
                }
            }
        }

        // Преобразуем Set в List для возврата
        return new ArrayList<>(uniqueAddresses);
    }

    public Optional<Order> getOrderWithDetails(Long id, User user) {
        return orderRepository.findByIdAndUser(id, user);
    }
}