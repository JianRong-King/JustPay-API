package com.justpay.transfer;

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
@Table(name = "transfers")
public class Transfer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String paymentRequestId;

	private Long fromAccountId;

	@Enumerated(EnumType.STRING)
	private Currency fromCurrency;

	private BigDecimal fromAmount;

	private Long toAccountId;

	@Enumerated(EnumType.STRING)
	private Currency toCurrency;

	private BigDecimal toAmount;

	private BigDecimal rate;

	private Instant createdAt;

	protected Transfer() {
	}

	public Transfer(String paymentRequestId, Long fromAccountId, Currency fromCurrency, BigDecimal fromAmount,
			Long toAccountId, Currency toCurrency, BigDecimal toAmount, BigDecimal rate, Instant createdAt) {
		this.paymentRequestId = paymentRequestId;
		this.fromAccountId = fromAccountId;
		this.fromCurrency = fromCurrency;
		this.fromAmount = fromAmount;
		this.toAccountId = toAccountId;
		this.toCurrency = toCurrency;
		this.toAmount = toAmount;
		this.rate = rate;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getPaymentRequestId() {
		return paymentRequestId;
	}

	public Long getFromAccountId() {
		return fromAccountId;
	}

	public Currency getFromCurrency() {
		return fromCurrency;
	}

	public BigDecimal getFromAmount() {
		return fromAmount;
	}

	public Long getToAccountId() {
		return toAccountId;
	}

	public Currency getToCurrency() {
		return toCurrency;
	}

	public BigDecimal getToAmount() {
		return toAmount;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
