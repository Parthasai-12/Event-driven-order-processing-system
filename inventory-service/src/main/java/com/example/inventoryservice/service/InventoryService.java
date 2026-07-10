package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.OrderEvent;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.kafka.InventoryEventProducer;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;

    private boolean failureModeEnabled = false;

    public void setFailureModeEnabled(boolean enabled) {
        this.failureModeEnabled = enabled;
    }

    public boolean isFailureModeEnabled() {
        return this.failureModeEnabled;
    }

    @Transactional
    public void reserveStock(OrderEvent event) {
        log.info("Processing stock reservation for orderId: {}, productId: {}, quantity: {}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        if (failureModeEnabled) {
            log.warn("Failure mode enabled in Inventory Service. Throwing retryable exception for order: {}", event.getOrderId());
            throw new RuntimeException("Simulated retryable error in Inventory Service");
        }

        Inventory inventory = inventoryRepository.findById(event.getProductId())
                .orElseGet(() -> {
                    log.info("Product {} not found in inventory, creating dynamic stock for testing", event.getProductId());
                    return Inventory.builder()
                            .productId(event.getProductId())
                            .productName("Dynamic Product " + event.getProductId())
                            .availableQuantity(1000)
                            .reservedQuantity(0)
                            .build();
                });

        boolean simulateFailure = Boolean.TRUE.equals(event.getSimulateInventoryFailure());
        boolean insufficientStock = inventory.getAvailableQuantity() < event.getQuantity();

        if (simulateFailure || insufficientStock) {
            log.warn("Inventory reservation failed for orderId: {}. SimulateFailure: {}, InsufficientStock: {} (Available: {}, Requested: {})",
                    event.getOrderId(), simulateFailure, insufficientStock, inventory.getAvailableQuantity(), event.getQuantity());
            
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
            
            inventoryEventProducer.publishInventoryFailed(failedEvent);
            return;
        }

        // Reserve stock
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - event.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + event.getQuantity());
        
        inventoryRepository.save(inventory);
        log.info("Reserved {} units of product {} for order {}", event.getQuantity(), event.getProductId(), event.getOrderId());

        // Publish inventory-reserved event
        OrderEvent reservedEvent = OrderEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .status("INVENTORY_RESERVED")
                .simulateInventoryFailure(event.getSimulateInventoryFailure())
                .simulatePaymentFailure(event.getSimulatePaymentFailure())
                .build();
        
        inventoryEventProducer.publishInventoryReserved(reservedEvent);
    }

    @Transactional
    public void compensateStock(OrderEvent event) {
        log.info("[SAGA COMPENSATION] Compensating stock for orderId: {}, productId: {}, quantity: {}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        inventoryRepository.findById(event.getProductId()).ifPresentOrElse(inventory -> {
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + event.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - event.getQuantity());
            inventoryRepository.save(inventory);
            log.info("[SAGA COMPENSATION] Restored stock. Available: {}, Reserved: {} for product: {}",
                    inventory.getAvailableQuantity(), inventory.getReservedQuantity(), event.getProductId());
        }, () -> log.error("[SAGA COMPENSATION] Product {} not found in inventory for compensation!", event.getProductId()));
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();
    }
}
