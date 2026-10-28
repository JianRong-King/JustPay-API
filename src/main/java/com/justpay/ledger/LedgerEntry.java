package com.justpay.ledger;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import com.justpay.common.Currency;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long accountId;

	@Enumerated(EnumType.STRING)
	private Currency currency;

	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private LedgerEntryType type;

	private Long transferId;

	private Instant createdAt;

	protected LedgerEntry() {
	}

	public LedgerEntry(Long accountId, Currency currency, BigDecimal amount, LedgerEntryType type,
			Long transferId, Instant createdAt) {
		this.accountId = accountId;
		this.currency = currency;
		this.amount = amount;
		this.type = type;
		this.transferId = transferId;
		this.createdAt = createdAt;
	}

	public static LedgerEntry deposit(Long accountId, Currency currency, BigDecimal amount, Instant createdAt) {
		return new LedgerEntry(accountId, currency, amount, LedgerEntryType.DEPOSIT, null, createdAt);
	}

	public static LedgerEntry transferOut(Long accountId, Currency currency, BigDecimal amount, Long transferId,
			Instant createdAt) {
		return new LedgerEntry(accountId, currency, amount.negate(), LedgerEntryType.TRANSFER_OUT, transferId,
				createdAt);
	}

	public static LedgerEntry transferIn(Long accountId, Currency currency, BigDecimal amount, Long transferId,
			Instant createdAt) {
		return new LedgerEntry(accountId, currency, amount, LedgerEntryType.TRANSFER_IN, transferId, createdAt);
	}

	public Long getId() {
		return id;
	}

	public Long getAccountId() {
		return accountId;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LedgerEntryType getType() {
		return type;
	}

	public Long getTransferId() {
		return transferId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
