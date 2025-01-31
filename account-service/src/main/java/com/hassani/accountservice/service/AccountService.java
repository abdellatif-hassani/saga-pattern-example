package com.hassani.accountservice.service;

import com.hassani.accountservice.entities.Account;
import com.hassani.accountservice.repository.AccountRepository;
import com.hassani.commonlib.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "initiate-transfer", groupId = "account-service")
    public void handleInitiateTransfer(InitiateTransfer event) {
        log.info("Received initiate transfer event: from={}, to={}, amount={}",
                event.getFromAccount(), event.getToAccount(), event.getAmount());
        try {
            debit(event.getFromAccount(), event.getAmount());
            log.info("Successfully debited amount {} from account {}",
                    event.getAmount(), event.getFromAccount());
        } catch (RuntimeException e) {
            log.error("Failed to process debit for account {}: {}",
                    event.getFromAccount(), e.getMessage());
            try {
                kafkaTemplate.send("transfer-failed",
                                new TransferFailed(event.getFromAccount(), event.getAmount()))
                        .get();
                log.info("Transfer failed event sent for account {}", event.getFromAccount());
            } catch (Exception ke) {
                log.error("Failed to send transfer-failed event: {}", ke.getMessage());
            }
        }
    }

    @KafkaListener(topics = "credit-account", groupId = "account-service")
    public void handleCreditAccount(CreditAccount event) {
        log.info("Received credit account event: account={}, amount={}",
                event.getAccountNumber(), event.getAmount());
        try {
            credit(event.getAccountNumber(), event.getAmount());
            log.info("Successfully credited amount {} to account {}",
                    event.getAmount(), event.getAccountNumber());
        } catch (RuntimeException e) {
            log.error("Failed to process credit for account {}: {}",
                    event.getAccountNumber(), e.getMessage());
            // Trigger compensation - revert the debit
            try {
                kafkaTemplate.send("credit-failed",
                                new CreditFailed(event.getAccountNumber(), event.getAmount()))
                        .get();
                log.info("Credit failed event sent for account {}", event.getAccountNumber());
            } catch (Exception ke) {
                log.error("Failed to send credit-failed event: {}", ke.getMessage());
            }
        }
    }

    @KafkaListener(topics = "revert-debit", groupId = "account-service")
    public void handleRevertDebit(RevertDebit event) {
        log.info("Received revert debit event for account: {}, amount: {}",
                event.getAccountNumber(), event.getAmount());
        try {
            credit(event.getAccountNumber(), event.getAmount()); // Revert the debit by crediting back
            log.info("Successfully reverted debit for account {}", event.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to revert debit for account {}: {}",
                    event.getAccountNumber(), e.getMessage());
        }
    }


    @Transactional
    public void debit(String accountNumber, Double amount) {
        log.debug("Attempting to debit {} from account {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found: {}", accountNumber);
                    return new RuntimeException("Account not found: " + accountNumber);
                });

        if (account.getBalance() < amount) {
            log.error("Insufficient balance in account {}: balance={}, required={}",
                    accountNumber, account.getBalance(), amount);
            throw new RuntimeException("Insufficient balance in account: " + accountNumber);
        }

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        log.debug("Account {} debited successfully. New balance: {}",
                accountNumber, account.getBalance());

        try {
            kafkaTemplate.send("account-debited",
                    new AccountDebited(accountNumber, amount)).get();
            log.info("Account debited event sent for account {}", accountNumber);
        } catch (Exception e) {
            log.error("Failed to send account-debited event: {}", e.getMessage());
            throw new RuntimeException("Failed to process debit transaction", e);
        }
    }

    @Transactional
    public void credit(String accountNumber, Double amount) {
        log.debug("Attempting to credit {} to account {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found: {}", accountNumber);
                    return new RuntimeException("Account not found: " + accountNumber);
                });

        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
        log.debug("Account {} credited successfully. New balance: {}",
                accountNumber, account.getBalance());

        try {
            kafkaTemplate.send("account-credited",
                    new AccountCredited(accountNumber, amount)).get();
            log.info("Account credited event sent for account {}", accountNumber);
        } catch (Exception e) {
            log.error("Failed to send account-credited event: {}", e.getMessage());
            throw new RuntimeException("Failed to process credit transaction", e);
        }
    }
}