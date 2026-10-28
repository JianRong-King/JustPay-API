package com.justpay.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.justpay.common.Currency;

import jakarta.persistence.LockModeType;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

	List<Balance> findByAccountIdOrderById(Long accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Balance> findForUpdateByAccountIdAndCurrency(Long accountId, Currency currency);

}
