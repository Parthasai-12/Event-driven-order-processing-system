package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderEvent;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for product: {}, quantity: {}", request.getProductId(), request.getQuantity());
        
        Order order = Order.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Publish event
        OrderEvent event = OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(savedOrder.getId())
                .productId(savedOrder.getProductId())
                .quantity(savedOrder.getQuantity())
                .amount(savedOrder.getAmount())
                .status(savedOrder.getStatus().name())
                .simulateInventoryFailure(request.getSimulateInventoryFailure())
                .simulatePaymentFailure(request.getSimulatePaymentFailure())
                .build();
        
        orderEventProducer.publishOrderCreated(event);
        
        return mapToResponse(savedOrder);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        log.info("Updating status for orderId: {} to {}", orderId, status);
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(OrderStatus.valueOf(status));
            orderRepository.save(order);
            log.info("Successfully updated orderId: {} status to {}", orderId, status);
        }, () -> log.error("Order not found with id: {}", orderId));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
