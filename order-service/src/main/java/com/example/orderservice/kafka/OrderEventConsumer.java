package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "inventory-reserved", groupId = "order-group")
    public void consumeInventoryReserved(OrderEvent event) {
        log.info("Received inventory-reserved event: {}", event);
        orderService.updateOrderStatus(event.getOrderId(), "INVENTORY_RESERVED");
    }

    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void consumePaymentSuccess(OrderEvent event) {
        log.info("Received payment-success event: {}", event);
        orderService.updateOrderStatus(event.getOrderId(), "COMPLETED");
    }

    @KafkaListener(topics = "inventory-failed", groupId = "order-group")
    public void consumeInventoryFailed(OrderEvent event) {
        log.info("Received inventory-failed event: {}", event);
        orderService.updateOrderStatus(event.getOrderId(), "FAILED");
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-group")
    public void consumePaymentFailed(OrderEvent event) {
        log.info("Received payment-failed event: {}", event);
        orderService.updateOrderStatus(event.getOrderId(), "FAILED");
    }
}
