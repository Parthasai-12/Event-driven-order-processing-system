package com.example.inventoryservice.kafka;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-created", groupId = "inventory-group")
    public void consumeOrderCreated(OrderEvent event) {
        log.info("Received order-created event: {}", event);
        inventoryService.reserveStock(event);
    }

    @KafkaListener(topics = "payment-failed", groupId = "inventory-group")
    public void consumePaymentFailed(OrderEvent event) {
        log.info("Received payment-failed event for stock compensation: {}", event);
        inventoryService.compensateStock(event);
    }
}
