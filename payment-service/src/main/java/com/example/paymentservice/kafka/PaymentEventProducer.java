package com.example.paymentservice.kafka;

import com.example.paymentservice.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishPaymentSuccess(OrderEvent event) {
        log.info("Publishing payment-success event: {}", event);
        kafkaTemplate.send("payment-success", String.valueOf(event.getOrderId()), event);
    }

    public void publishPaymentFailed(OrderEvent event) {
        log.info("Publishing payment-failed event: {}", event);
        kafkaTemplate.send("payment-failed", String.valueOf(event.getOrderId()), event);
    }
}
