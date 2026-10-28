package com.justpay.ledger;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.justpay.common.Currency;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

	List<LedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

	List<LedgerEntry> findByAccountIdAndCurrencyOrderByCreatedAtDescIdDesc(Long accountId, Currency currency);

}
