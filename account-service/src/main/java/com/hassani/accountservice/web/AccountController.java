package com.hassani.accountservice.web;

import com.hassani.accountservice.model.CreditRequest;
import com.hassani.accountservice.model.DebitRequest;
import com.hassani.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/debit")
    public ResponseEntity<String> debitAccount(@RequestBody DebitRequest request) {
        accountService.debit(request.getAccountNumber(), request.getAmount());
        return ResponseEntity.ok("Account debited successfully");
    }

    @PostMapping("/credit")
    public ResponseEntity<String> creditAccount(@RequestBody CreditRequest request) {
        accountService.credit(request.getAccountNumber(), request.getAmount());
        return ResponseEntity.ok("Account credited successfully");
    }
}
