package org.ikigaidigital.adapter.persistence.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "time_deposits")
public class TimeDepositEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_type", nullable = false, length = 100)
    private String planType;

    // Flyway owns the schema. An unrestricted NUMERIC must not add rounding to the legacy result.
    @Column(nullable = false, columnDefinition = "numeric")
    @Setter
    private BigDecimal balance;

    @Column(nullable = false)
    private int days;

    @OneToMany(mappedBy = "timeDeposit")
    @OrderBy("id ASC")
    private List<WithdrawalEntity> withdrawals = new ArrayList<>();
}
