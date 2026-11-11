package org.ikigaidigital.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Shared mutable calculation model; preserves the constructor and accessor API from the kata. */
@Getter
@AllArgsConstructor
public class TimeDeposit {
    private int id;
    private String planType;
    @Setter
    private Double balance;
    private int days;
}
