package com.example.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private String eventId;
    private Boolean simulateInventoryFailure;
    private Boolean simulatePaymentFailure;
}
