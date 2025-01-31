package com.hassani.commonlib.event;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditFailed {
    private String accountNumber;
    private Double amount;
}