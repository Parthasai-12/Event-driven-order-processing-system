package com.example.orderservice.entity;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSING,
    COMPLETED,
    FAILED
}
