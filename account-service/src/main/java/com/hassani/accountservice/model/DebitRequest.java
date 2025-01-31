package com.hassani.accountservice.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DebitRequest {
    private String accountNumber;
    private Double amount;
}
