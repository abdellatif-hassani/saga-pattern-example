package com.hassani.commonlib.event;

import lombok.*;

@AllArgsConstructor @NoArgsConstructor @Getter
@Setter
@ToString
public class RevertDebit {
    private String accountNumber;
    private Double amount;
}