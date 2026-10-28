package com.justpay.fx;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import com.justpay.common.Currency;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "fx_rates")
public class FxRate {

	@Id
	private Long id;

	@Enumerated(EnumType.STRING)
	private Currency fromCurrency;

	@Enumerated(EnumType.STRING)
	private Currency toCurrency;

	private BigDecimal rate;

	protected FxRate() {
	}

	public Long getId() {
		return id;
	}

	public Currency getFromCurrency() {
		return fromCurrency;
	}

	public Currency getToCurrency() {
		return toCurrency;
	}

	public BigDecimal getRate() {
		return rate;
	}

}
