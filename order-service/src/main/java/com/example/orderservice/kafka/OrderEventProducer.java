package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderCreated(OrderEvent event) {
        log.info("Publishing order-created event: {}", event);
        kafkaTemplate.send("order-created", String.valueOf(event.getOrderId()), event);
    }
}
