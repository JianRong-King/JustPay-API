package com.justpay.account;

import java.math.BigDecimal;

import com.justpay.common.Currency;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "balances")
public class Balance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long accountId;

	@Enumerated(EnumType.STRING)
	private Currency currency;

	private BigDecimal amount;

	protected Balance() {
	}

	public Balance(Long accountId, Currency currency) {
		this.accountId = accountId;
		this.currency = currency;
		this.amount = BigDecimal.ZERO.setScale(2);
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

	public void add(BigDecimal value) {
		this.amount = this.amount.add(value);
	}

	public void subtract(BigDecimal value) {
		this.amount = this.amount.subtract(value);
	}

}
