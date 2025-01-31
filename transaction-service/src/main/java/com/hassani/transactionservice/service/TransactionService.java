package com.hassani.transactionservice.service;

import com.hassani.commonlib.event.*;
import com.hassani.transactionservice.entities.Transaction;
import com.hassani.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void initiateTransfer(String fromAccount, String toAccount, Double amount) {
        log.info("Initiating transfer: from={}, to={}, amount={}", fromAccount, toAccount, amount);
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus("PENDING");
        transaction = transactionRepository.save(transaction);
        log.info("Created transaction with ID: {}, status: {}", transaction.getId(), transaction.getStatus());

        kafkaTemplate.send("initiate-transfer", new InitiateTransfer(fromAccount, toAccount, amount));
        log.info("Sent initiate-transfer event for transaction ID: {}", transaction.getId());
    }

    @KafkaListener(topics = "account-debited", groupId = "transaction-service")
    public void handleAccountDebited(AccountDebited event) {
        log.info("Received account-debited event: account={}, amount={}",
                event.getAccountNumber(), event.getAmount());

        try {
            Transaction transaction = transactionRepository
                    .findFirstByFromAccountAndStatusOrderByIdDesc(event.getAccountNumber(), "PENDING")
                    .orElseThrow(() -> new RuntimeException("No pending transaction found for account: " + event.getAccountNumber()));

            log.info("Found transaction ID: {}, updating status to DEBITED", transaction.getId());
            transaction.setStatus("DEBITED");
            transactionRepository.save(transaction);

            kafkaTemplate.send("credit-account",
                    new CreditAccount(transaction.getToAccount(), transaction.getAmount()));
            log.info("Sent credit-account event for transaction ID: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Error processing account-debited event: {}", e.getMessage());
            throw new RuntimeException("Failed to process account-debited event", e);
        }
    }

    @KafkaListener(topics = "account-credited", groupId = "transaction-service")
    public void handleAccountCredited(AccountCredited event) {
        log.info("Received account-credited event: account={}, amount={}",
                event.getAccountNumber(), event.getAmount());

        try {
            // First try to find a transaction where this account is the destination
            Optional<Transaction> transactionOpt = transactionRepository
                    .findFirstByToAccountAndStatusOrderByIdDesc(event.getAccountNumber(), "DEBITED");

            if (transactionOpt.isPresent()) {
                // Normal flow - completing a successful transfer
                Transaction transaction = transactionOpt.get();
                log.info("Found transaction ID: {}, updating status to COMPLETED", transaction.getId());
                transaction.setStatus("COMPLETED");
                transactionRepository.save(transaction);
                log.info("Transaction ID: {} completed successfully", transaction.getId());
            } else {
                // Check if this is a compensation credit (rollback)
                transactionOpt = transactionRepository
                        .findFirstByFromAccountAndStatusOrderByIdDesc(event.getAccountNumber(), "FAILED");

                if (transactionOpt.isPresent()) {
                    Transaction transaction = transactionOpt.get();
                    log.info("Found failed transaction ID: {}, compensation completed", transaction.getId());
                } else {
                    log.warn("No matching transaction found for account-credited event: account={}",
                            event.getAccountNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error processing account-credited event: {}", e.getMessage());
            throw new RuntimeException("Failed to process account-credited event", e);
        }
    }

    @KafkaListener(topics = "transfer-failed", groupId = "transaction-service")
    public void handleTransferFailed(TransferFailed event) {
        log.info("Received transfer-failed event: account={}, amount={}",
                event.getFromAccount(), event.getAmount());

        try {
            Transaction transaction = transactionRepository
                    .findFirstByFromAccountAndStatusOrderByIdDesc(event.getFromAccount(), "PENDING")
                    .orElseThrow(() -> new RuntimeException("No pending transaction found for account: " + event.getFromAccount()));

            log.info("Found transaction ID: {}, updating status to FAILED", transaction.getId());
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            log.info("Transaction ID: {} marked as failed", transaction.getId());
        } catch (Exception e) {
            log.error("Error processing transfer-failed event: {}", e.getMessage());
            throw new RuntimeException("Failed to process transfer-failed event", e);
        }
    }

    @KafkaListener(topics = "credit-failed", groupId = "transaction-service")
    public void handleCreditFailed(CreditFailed event) {
        log.info("Received credit-failed event: account={}, amount={}",
                event.getAccountNumber(), event.getAmount());

        try {
            Transaction transaction = transactionRepository
                    .findFirstByToAccountAndStatusOrderByIdDesc(event.getAccountNumber(), "DEBITED")
                    .orElseThrow(() -> new RuntimeException("No debited transaction found for account: " + event.getAccountNumber()));

            log.info("Found transaction ID: {}, updating status to FAILED", transaction.getId());
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            kafkaTemplate.send("revert-debit",
                    new RevertDebit(transaction.getFromAccount(), transaction.getAmount()));
            log.info("Sent revert-debit event for transaction ID: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Error processing credit-failed event: {}", e.getMessage());
            throw new RuntimeException("Failed to process credit-failed event", e);
        }
    }
}