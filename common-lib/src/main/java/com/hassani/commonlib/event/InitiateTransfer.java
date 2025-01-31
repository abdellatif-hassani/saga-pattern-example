package com.hassani.commonlib.event;

import lombok.*;

@AllArgsConstructor @NoArgsConstructor @Getter
@Setter
@ToString
public class InitiateTransfer {
    private String fromAccount;
    private String toAccount;
    private Double amount;
}