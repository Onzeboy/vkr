package com.nzby.homeshop.Services;

import com.nzby.homeshop.DTO.AssignedOrderDTO;
import com.nzby.homeshop.DTO.OrderDTO;
import com.nzby.homeshop.DTO.ProcessedOrdersDTO;
import com.nzby.homeshop.POJO.*;
import com.nzby.homeshop.POJO.Enum.OrderStatus;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.Repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger Logger = LoggerFactory.getLogger(OrderService.class);

    private static final int MAX_ACTIVE_ORDERS_PER_COURIER = 5;



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

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private UserService userService;

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
        order.setStatus(OrderStatus.PENDING);
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


    public List<Order> getOrdersWithImages(User user) {
        List<Order> orders = orderRepository.findByUserWithItemsAndProducts(user);

        List<Product> products = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getProduct)
                .filter(Objects::nonNull) // Ensure no null products
                .distinct()
                .collect(Collectors.toList());

        if (!products.isEmpty()) {
            orderRepository.findProductsWithImages(products);
        }

        orders.forEach(order -> {
            Logger.info("Заказ: {}, Пользователь: {}, Элементы заказа: {}, Продукт: {}, Изображения: {}",
                    order.getId(),
                    user.getId(),
                    order.getOrderItems() != null ? order.getOrderItems().size() : 0,
                    order.getOrderItems() != null && !order.getOrderItems().isEmpty() && order.getOrderItems().get(0).getProduct() != null
                            ? order.getOrderItems().get(0).getProduct().getName()
                            : "Нет продукта",
                    order.getOrderItems() != null && !order.getOrderItems().isEmpty() && order.getOrderItems().get(0).getProduct() != null
                            && order.getOrderItems().get(0).getProduct().getImages() != null
                            ? order.getOrderItems().get(0).getProduct().getImages().size()
                            : "Нет изображений");
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

    public Order getLastOrderByUser(User user) {
        try {
            Logger.debug("Получение последнего заказа для пользователя {}", user.getId());
            return orderRepository.findTopByUserOrderByCreatedAtDesc(user);
        } catch (IncorrectResultSizeDataAccessException e) {
            Logger.warn("Найдено несколько заказов для пользователя {}, выбираем первый. Причина: {}", user.getId(), e.getMessage());
            List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            Logger.debug("Найдено {} заказов для пользователя {}", orders.size(), user.getId());
            return orders.isEmpty() ? null : orders.get(0);
        }
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        if (status == null) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.findByStatus(status, pageable);
    }

    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ с ID " + orderId + " не найден"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    public ProcessedOrdersDTO getProcessedOrdersCount() {
        LocalDateTime startOfDay = LocalDateTime.now();
        ProcessedOrdersDTO dto = new ProcessedOrdersDTO();
        dto.setTodayCount(orderRepository.countByStatusAndCreatedAtAfter(OrderStatus.DELIVERED, startOfDay));
        dto.setTotalCount(orderRepository.countByStatus(OrderStatus.DELIVERED));
        return dto;
    }

    public ProcessedOrdersDTO getProcessedOrdersCountForCourier(Courier courier) {
        LocalDateTime startOfDay = LocalDateTime.now();
        ProcessedOrdersDTO dto = new ProcessedOrdersDTO();
        dto.setTodayCount(orderRepository.countByStatusAndCreatedAtAfterAndCourier(OrderStatus.DELIVERED, startOfDay, courier));
        dto.setTotalCount(orderRepository.countByStatusAndCourier(OrderStatus.DELIVERED, courier));
        return dto;
    }

    public List<Order> getProcessedOrdersForCourier(Courier courier) {
        LocalDateTime startOfDay = LocalDateTime.now();
        return orderRepository.findByStatusAndCreatedAtAfterAndCourier(OrderStatus.PENDING, startOfDay, courier);
    }

    public List<Order> getAllProcessedOrdersForCourier(Courier courier) {
        return orderRepository.findByStatusAndCourier(OrderStatus.PENDING, courier);
    }

    public List<Order> getAllActiveOrdersForCourier(Courier courier) {
        return orderRepository.findByCourierAndStatusIn(courier, Arrays.asList(OrderStatus.ASSIGNED, OrderStatus.IN_DELIVERY));
    }
    public List<Order> getAllDeliveredOrdersForCourier(Courier courier) {
        return orderRepository.findByCourierAndStatus(courier, OrderStatus.DELIVERED);
    }
    public List<Order> getActiveOrdersForCourierToday(Courier courier) {
        LocalDateTime startOfDay = LocalDateTime.now();
        return orderRepository.findActiveByCreatedAtAfterAndCourier(OrderStatus.PENDING, startOfDay, courier);
    }

    public List<AssignedOrderDTO> getAllAssignedOrders() {
        return orderRepository.findAllByStatus(OrderStatus.ASSIGNED)
                .stream()
                .map(order -> new AssignedOrderDTO(
                        order.getId(),
                        order.getOrderDate(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getTotalWeightInGrams()))
                .collect(Collectors.toList());
    }
    public List<Courier> getAllCouriers() {
        List<User> courierUsers = userRepository.findByRole(Role.COURIER);
        return courierUsers.stream()
                .map(User::getCourier)
                .filter(courier -> courier != null && courier.getId() != null)
                .collect(Collectors.toList());
    }

    @Transactional
    public Order assignCourierToOrder(Long orderId, Long courierId) {
        Order order = getOrderById(orderId);

        // Проверка, что заказ еще не назначен
        if (order.getCourier() != null) {
            throw new IllegalArgumentException("Заказ #" + orderId + " уже назначен курьеру");
        }

        // Проверка статуса заказа
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Заказ #" + orderId + " не в статусе 'PENDING'");
        }

        if (courierId != null) {
            // Поиск курьера
            Courier courier = courierRepository.findById(courierId)
                    .orElseThrow(() -> new IllegalArgumentException("Курьер с ID " + courierId + " не найден"));

            // Проверка количества активных заказов курьера
            List<Order> activeOrders = orderRepository.findByCourierAndStatusIn(
                    courier, Arrays.asList(OrderStatus.ASSIGNED, OrderStatus.IN_DELIVERY));
            if (activeOrders.size() >= MAX_ACTIVE_ORDERS_PER_COURIER) {
                throw new IllegalArgumentException("Курьер #" + courierId + " уже имеет максимум " +
                        MAX_ACTIVE_ORDERS_PER_COURIER + " активных заказов");
            }

            // Назначение курьера
            order.setCourier(courier);
            order.setStatus(OrderStatus.ASSIGNED);
            order.setUpdatedAt(LocalDateTime.now());
        } else {
            order.setCourier(null);
            order.setUpdatedAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    public List<OrderDTO> getProcessedOrdersDTO(Courier courier) {
        return getProcessedOrdersForCourier(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllProcessedOrdersDTO(Courier courier) {
        return getAllProcessedOrdersForCourier(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getActiveOrdersDTO(Courier courier) {
        return getActiveOrdersForCourierToday(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllActiveOrdersDTO(Courier courier) {
        return getAllActiveOrdersForCourier(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());
    }

    public List<Order> getAllAssignedOrdersForCourier(Courier courier) {
        return orderRepository.findAssignedByCourier(OrderStatus.ASSIGNED, courier);
    }

    public Order findById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Transactional
    public void cancelOrder(Long id, User user) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalStateException("Заказ не найден или не принадлежит пользователю"));
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new IllegalArgumentException("Отменить можно только заказ в статусе 'В обработке'");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        Logger.info("Заказ #{} успешно отменен", id);
    }

    @Transactional
    public void repeatOrder(Long id, User user) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalStateException("Заказ не найден или не принадлежит пользователю"));
        if (!order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new IllegalArgumentException("Повторить можно только доставленный заказ");
        }

        List<CartItem> existingCartItems = cartService.getCartItemsByUser(user);
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getProduct() == null) {
                Logger.warn("Товар в заказе #{} отсутствует, пропускаем", id);
                continue;
            }
            boolean itemExistsInCart = existingCartItems.stream()
                    .anyMatch(cartItem -> cartItem.getProduct().getId().equals(orderItem.getProduct().getId()));
            if (!itemExistsInCart) {
                CartItem newCartItem = new CartItem();
                newCartItem.setUser(user);
                newCartItem.setProduct(orderItem.getProduct());
                newCartItem.setQuantity(orderItem.getQuantity());
                BigDecimal price = orderItem.getPrice() != null ? orderItem.getPrice() : orderItem.getProduct().getPrice();
                if (price == null) {
                    Logger.warn("Цена для товара {} в заказе #{} отсутствует, пропускаем", orderItem.getProduct().getId(), id);
                    continue;
                }
                newCartItem.setUnitPrice(price);
                newCartItem.setOrdered(true);
                cartItemRepository.save(newCartItem);
            } else {
                Logger.debug("Товар {} уже в корзине, пропускаем", orderItem.getProduct().getId());
            }
        }
        Logger.info("Товары из заказа #{} добавлены в корзину", id);
    }

    public void save(Order order) {
        orderRepository.save(order);
    }


    public List<OrderDTO> getAllDeliveredOrdersDTO(Courier courier) {
        return getAllDeliveredOrdersForCourier(courier).stream()
                .map(order -> new OrderDTO(
                        order.getId(),
                        order.getUser().getName() + " " + order.getUser().getSurname(),
                        order.getAddress().getCity() + ", " + order.getAddress().getStreetAddress(),
                        order.getTotalAmount(),
                        order.getStatus().name(),
                        order.getCreatedAt(),
                        order.getCourier() != null ? order.getCourier().getUser().getName() + " " + order.getCourier().getUser().getSurname() : "Не назначен"))
                .collect(Collectors.toList());

    }
}