package com.example.inventoryservice.kafka;

import com.example.inventoryservice.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishInventoryReserved(OrderEvent event) {
        log.info("Publishing inventory-reserved event: {}", event);
        kafkaTemplate.send("inventory-reserved", String.valueOf(event.getOrderId()), event);
    }

    public void publishInventoryFailed(OrderEvent event) {
        log.info("Publishing inventory-failed event: {}", event);
        kafkaTemplate.send("inventory-failed", String.valueOf(event.getOrderId()), event);
    }
}
