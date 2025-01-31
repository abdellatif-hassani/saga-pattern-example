# SAGA Pattern Implementation in Spring Boot

This project demonstrates the implementation of the **SAGA pattern** using **Spring Boot** and **Kafka** for managing distributed transactions across microservices. It ensures data consistency and fault tolerance during account transfers.

## Features
- **Account Service**: Manages account balances, handles debit/credit operations, and publishes/consumes Kafka events.
- **Transaction Service**: Orchestrates the transfer process, listens to Kafka events, and ensures transaction consistency.

## Technologies Used
- **Spring Boot**: Backend framework for building microservices.
- **Kafka**: Event-driven communication between services.
- **Lombok**: Simplifies Java code with annotations.
- **H2 Database**: In-memory database for testing.

## How It Works
1. **Initiate Transfer**: The `TransactionService` initiates a transfer and publishes an `initiate-transfer` event.
2. **Debit Account**: The `AccountService` listens to the event, debits the sender's account, and publishes an `account-debited` event.
3. **Credit Account**: The `TransactionService` listens to the `account-debited` event and publishes a `credit-account` event to credit the receiver's account.
4. **Compensation**: If any step fails, compensation events like `transfer-failed` or `revert-debit` are triggered to roll back the transaction.

## Getting Started
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/saga-pattern-spring-boot.git
    ```
2. Start Kafka and Zookeeper:
    ```bash
    docker-compose up -d
    ```
3. Run the Spring Boot applications:
   - AccountServiceApplication
   - TransactionServiceApplication