# Event-Driven Order Processing Platform - Phase 2

This repository contains Phase 2 of the Event-Driven Order Processing Platform, implementing comprehensive failure handling, transactional Saga pattern compensation logic, Spring Kafka retry mechanisms with exponential backoff, Dead Letter Queue (DLQ) routing, a dedicated Notification Service, and deterministic simulation endpoints.

---

## Architecture and Design Decisions

This system uses a **choreographed Saga pattern** to maintain data consistency across distributed database schemas:

1. **Order Service** creates orders with status `PENDING` and publishes an `order-created` event.
2. **Inventory Service** consumes `order-created` and attempts to reserve stock:
   - On success: Deducts stock, adds to reservations, and publishes `inventory-reserved`.
   - On failure (out of stock or simulated): Publishes `inventory-failed`.
3. **Payment Service** consumes `inventory-reserved` and attempts payment:
   - On success: Saves payment details and publishes `payment-success`.
   - On failure (simulated): Saves failed payment details and publishes `payment-failed`.
4. **Saga Compensation**:
   - **Inventory Service** consumes `payment-failed` and restores stock (`available_quantity += quantity`, `reserved_quantity -= quantity`).
   - **Order Service`** consumes `payment-failed` and `inventory-failed` to update order status to `FAILED`.
   - **Notification Service** consumes `payment-success` and `payment-failed` to store logs and simulate SMS & Email.

---

## Service Port Map

| Service | Port | Description | Database Schema |
| :--- | :--- | :--- | :--- |
| **Order Service** | `8081` | Rest API to place and track orders | `order_db` |
| **Inventory Service** | `8082` | Rest API to view stock levels and toggle failure mode | `inventory_db` |
| **Payment Service** | `8083` | Rest API to view payment transactions and toggle failure mode | `payment_db` |
| **Notification Service** | `8084` | Rest API to view delivery notification logs | `notification_db` |
| **Kafka Broker** | `9092` / `29092` | Event-driven message broker running in KRaft mode | N/A |
| **MySQL Database** | `3307` (host) | Shared DB container containing dedicated schemas | N/A |

---

## API Documentation

### 1. Order Service (`8081`)
* **Create Order**: `POST /orders`
  ```json
  {
    "productId": 1,
    "quantity": 2,
    "amount": 5000,
    "simulateInventoryFailure": false,
    "simulatePaymentFailure": false
  }
  ```
* **Get All Orders**: `GET /orders`
* **Get Order by ID**: `GET /orders/{id}`
* **Swagger UI**: http://localhost:8081/swagger-ui.html

### 2. Inventory Service (`8082`)
* **Get All Stock Levels**: `GET /inventory`
* **Toggle Failure Mode**: `PUT /inventory/failure-mode`
  ```json
  {
    "enabled": true
  }
  ```
* **Swagger UI**: http://localhost:8082/swagger-ui.html

### 3. Payment Service (`8083`)
* **Get All Payments**: `GET /payments`
* **Toggle Failure Mode**: `PUT /payments/failure-mode`
  ```json
  {
    "enabled": true
  }
  ```
* **Swagger UI**: http://localhost:8083/swagger-ui.html

### 4. Notification Service (`8084`)
* **Get All Notifications**: `GET /notifications`
* **Get Notifications by Order**: `GET /notifications/order/{orderId}`
* **Swagger UI**: http://localhost:8084/swagger-ui.html

---

## Step-by-Step Testing Scenarios

Use the following HTTP requests to verify the implementation after starting the containers.

### Scenario 1: Happy Path Execution
1. Send request:
   ```bash
   POST http://localhost:8081/orders
   {
     "productId": 1,
     "quantity": 5,
     "amount": 12000,
     "simulateInventoryFailure": false,
     "simulatePaymentFailure": false
   }
   ```
2. Verify:
   - Order status goes from `PENDING` -> `INVENTORY_RESERVED` -> `COMPLETED`.
   - Inventory: available stock drops by 5, reserved stock is incremented by 5 during processing, and remains reserved upon successful checkout.
   - Payment logs: SUCCESS log created in `payment_db`.
   - Notifications: A log is generated in `notification_db` stating: `"Payment successful for Order X"`. Check email and SMS simulations in logs.

---

### Scenario 2: Out of Stock (Natural Inventory Failure)
1. Product 1 starts with 100 available stock. Send a request that exceeds the available stock limit:
   ```bash
   POST http://localhost:8081/orders
   {
     "productId": 1,
     "quantity": 150,
     "amount": 360000
   }
   ```
2. Verify:
   - Inventory Service publishes `inventory-failed` without modifying database stock values.
   - Order Service receives the event and updates Order status to `FAILED`.

---

### Scenario 3: Simulated Inventory Failure
1. Send request:
   ```bash
   POST http://localhost:8081/orders
   {
     "productId": 2,
     "quantity": 3,
     "amount": 900,
     "simulateInventoryFailure": true
   }
   ```
2. Verify:
   - Inventory Service intercepts simulation flag, bypasses reservation, and publishes `inventory-failed`.
   - Order status is set to `FAILED`.
   - No stock deduction occurs in `inventory_db`.

---

### Scenario 4: Simulated Payment Failure & Saga Compensation
1. Send request:
   ```bash
   POST http://localhost:8081/orders
   {
     "productId": 3,
     "quantity": 10,
     "amount": 15000,
     "simulatePaymentFailure": true
   }
   ```
2. Verify:
   - Inventory Service successfully reserves stock: Available decreases by 10, Reserved increases by 10. Publishes `inventory-reserved`.
   - Order Service updates status to `INVENTORY_RESERVED`.
   - Payment Service consumes the reservation, checks the simulation flag, marks payment status as `FAILED` in `payments` table, and publishes `payment-failed`.
   - **Compensation**:
     - Inventory Service consumes `payment-failed` and restores stock: Available increases by 10, Reserved decreases by 10.
     - Order Service consumes `payment-failed` and updates status to `FAILED`.
     - Notification Service consumes `payment-failed` and logs `"Payment failed for Order X"`. Simulated SMS and Email logs are written to stdout.

---

### Scenario 5: Retries, Exponential Backoff, and Dead Letter Queue (DLQ)
1. Enable failure mode on Payment Service:
   ```bash
   PUT http://localhost:8083/payments/failure-mode
   {
     "enabled": true
   }
   ```
2. Submit a normal checkout request:
   ```bash
   POST http://localhost:8081/orders
   {
     "productId": 1,
     "quantity": 1,
     "amount": 2500
   }
   ```
3. Verify:
   - Inventory is reserved normally.
   - Payment Service consumes the reservation. Since failure mode is enabled, it throws a `RuntimeException`.
   - Spring Kafka's `CommonErrorHandler` catches the exception and schedules retries:
     - **Attempt 1**: Delay = 1 second
     - **Attempt 2**: Delay = 2 seconds
     - **Attempt 3**: Delay = 4 seconds
   - After the 3rd retry attempt fails, the event is routed to the DLT topic `payment-dlt`.
   - **DLQ Consumer**: `DltConsumer` in `payment-service` reads the failed message from `payment-dlt` and persists details into the `failed_events` database table (including original payload, event UUID, topic, error message, and timestamp).
