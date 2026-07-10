package com.example.paymentservice.kafka;

import com.example.paymentservice.dto.OrderEvent;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    private final PaymentService paymentService;

    @KafkaListener(topics = "inventory-reserved", groupId = "payment-group")
    public void consumeInventoryReserved(OrderEvent event) {
        log.info("Received inventory-reserved event: {}", event);
        paymentService.processPayment(event);
    }
}
