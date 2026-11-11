package org.ikigaidigital.adapter.persistence;

import jakarta.persistence.LockModeType;
import org.ikigaidigital.adapter.persistence.data.TimeDepositEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TimeDepositRepository extends JpaRepository<TimeDepositEntity, Integer> {
    @Query("""
        select distinct deposit from TimeDepositEntity deposit
        left join fetch deposit.withdrawals
        order by deposit.id
        """)
    List<TimeDepositEntity> findAllWithWithdrawals();

    // Lock deposits separately: PostgreSQL cannot lock the nullable side of a collection join.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select deposit from TimeDepositEntity deposit order by deposit.id")
    List<TimeDepositEntity> findAllForUpdate();
}
