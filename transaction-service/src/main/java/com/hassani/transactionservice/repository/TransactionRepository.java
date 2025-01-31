package com.hassani.transactionservice.repository;

import com.hassani.transactionservice.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findFirstByFromAccountAndStatusOrderByIdDesc(String fromAccount, String status);
    Optional<Transaction> findFirstByToAccountAndStatusOrderByIdDesc(String toAccount, String status);
}