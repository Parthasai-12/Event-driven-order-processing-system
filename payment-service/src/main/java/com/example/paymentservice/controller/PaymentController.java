package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Endpoints for checking payment transaction details")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "Get all payment transaction logs")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PutMapping("/failure-mode")
    @Operation(summary = "Toggle failure mode for testing retries and DLQ")
    public ResponseEntity<java.util.Map<String, Object>> toggleFailureMode(@RequestBody java.util.Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", false);
        paymentService.setFailureModeEnabled(enabled);
        return ResponseEntity.ok(java.util.Map.of("message", "Payment failure mode set to " + enabled));
    }
}
