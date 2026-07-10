package com.example.paymentservice.service;

import com.example.paymentservice.dto.OrderEvent;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.kafka.PaymentEventProducer;
import com.example.paymentservice.repository.PaymentRepository;
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
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    private boolean failureModeEnabled = false;

    public void setFailureModeEnabled(boolean enabled) {
        this.failureModeEnabled = enabled;
    }

    public boolean isFailureModeEnabled() {
        return this.failureModeEnabled;
    }

    @Transactional
    public void processPayment(OrderEvent event) {
        log.info("Processing payment for orderId: {}, amount: {}", event.getOrderId(), event.getAmount());

        if (failureModeEnabled) {
            log.warn("Failure mode enabled in Payment Service. Throwing retryable exception for order: {}", event.getOrderId());
            throw new RuntimeException("Simulated retryable error in Payment Service");
        }

        boolean simulateFailure = Boolean.TRUE.equals(event.getSimulatePaymentFailure());

        if (simulateFailure) {
            log.warn("Payment simulation failed for orderId: {}", event.getOrderId());
            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getAmount())
                    .status(PaymentStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            log.info("Saved failed payment for orderId: {}", event.getOrderId());

            // Publish payment-failed event
            OrderEvent failedEvent = OrderEvent.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(event.getAmount())
                    .status("FAILED")
                    .simulateInventoryFailure(event.getSimulateInventoryFailure())
                    .simulatePaymentFailure(event.getSimulatePaymentFailure())
                    .build();

            paymentEventProducer.publishPaymentFailed(failedEvent);
            return;
        }

        // Simulate successful payment
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getAmount())
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Saved successful payment for orderId: {}", event.getOrderId());

        // Publish payment-success event
        OrderEvent successEvent = OrderEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .status("COMPLETED")
                .simulateInventoryFailure(event.getSimulateInventoryFailure())
                .simulatePaymentFailure(event.getSimulatePaymentFailure())
                .build();

        paymentEventProducer.publishPaymentSuccess(successEvent);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
