package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Inventory Controller", description = "Endpoints for checking product inventory stock levels")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get all inventory stock levels")
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/products")
    @Operation(summary = "Get all products for testing catalog")
    public ResponseEntity<List<InventoryResponse>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @PutMapping("/failure-mode")
    @Operation(summary = "Toggle failure mode for testing retries and DLQ")
    public ResponseEntity<java.util.Map<String, Object>> toggleFailureMode(@RequestBody java.util.Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", false);
        inventoryService.setFailureModeEnabled(enabled);
        return ResponseEntity.ok(java.util.Map.of("message", "Inventory failure mode set to " + enabled));
    }
}
