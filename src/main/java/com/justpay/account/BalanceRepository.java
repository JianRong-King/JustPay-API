package com.justpay.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.justpay.common.Currency;

import jakarta.persistence.LockModeType;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

	List<Balance> findByAccountIdOrderById(Long accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Balance> findForUpdateByAccountIdAndCurrency(Long accountId, Currency currency);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Balance> findForUpdateById(Long id);

	@Query("select b.id from Balance b where b.accountId = :accountId and b.currency = :currency")
	Optional<Long> findIdByAccountIdAndCurrency(Long accountId, Currency currency);

}
