package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "payment-success", groupId = "notification-group")
    public void consumePaymentSuccess(OrderEvent event) {
        log.info("Notification Service consumed payment-success event: {}", event);
        notificationService.processNotification(event, "PAYMENT_SUCCESS");
    }

    @KafkaListener(topics = "payment-failed", groupId = "notification-group")
    public void consumePaymentFailed(OrderEvent event) {
        log.info("Notification Service consumed payment-failed event: {}", event);
        notificationService.processNotification(event, "PAYMENT_FAILED");
    }
}
