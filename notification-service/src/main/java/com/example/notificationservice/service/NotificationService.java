package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderEvent;
import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.repository.NotificationRepository;
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
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void processNotification(OrderEvent event, String eventType) {
        log.info("Processing notification logs for orderId: {}, eventType: {}", event.getOrderId(), eventType);

        String message;
        if ("PAYMENT_SUCCESS".equalsIgnoreCase(eventType)) {
            message = "Payment successful for Order " + event.getOrderId();
        } else if ("PAYMENT_FAILED".equalsIgnoreCase(eventType)) {
            message = "Payment failed for Order " + event.getOrderId();
        } else {
            message = "Update received for Order " + event.getOrderId() + ": status " + event.getStatus();
        }

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .eventType(eventType)
                .message(message)
                .status(NotificationStatus.SENT) // Defaulting to SENT upon successful simulation
                .createdAt(LocalDateTime.now())
                .build();

        // Persist notification log
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification log with ID: {} for orderId: {}", savedNotification.getId(), event.getOrderId());

        // Trigger simulations
        sendEmail(savedNotification);
        sendSms(savedNotification);
    }

    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getNotificationsByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void sendEmail(Notification notification) {
        log.info("[SIMULATION - EMAIL] To: customer@example.com | Message: '{}' | Status: SENT", notification.getMessage());
    }

    private void sendSms(Notification notification) {
        log.info("[SIMULATION - SMS] To: +1-555-0199 | Message: '{}' | Status: SENT", notification.getMessage());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .orderId(notification.getOrderId())
                .eventType(notification.getEventType())
                .message(notification.getMessage())
                .status(notification.getStatus().name())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
