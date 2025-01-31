package com.hassani.commonlib.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditAccount {
    private String accountNumber;
    private Double amount;
}
