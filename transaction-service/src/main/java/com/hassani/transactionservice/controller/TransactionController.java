package com.hassani.transactionservice.controller;

import com.hassani.transactionservice.model.InitiateTransferRequest;
import com.hassani.transactionservice.service.TransactionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/initiate")
    public ResponseEntity<String> initiateTransfer(@RequestBody InitiateTransferRequest request) {
        transactionService.initiateTransfer(request.getFromAccount(), request.getToAccount(), request.getAmount());
        return ResponseEntity.ok("Transfer initiated successfully");
    }
}
