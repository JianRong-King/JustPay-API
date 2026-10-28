package com.justpay.account;

import java.math.BigDecimal;

import com.justpay.common.Currency;

public record BalanceResponse(Currency currency, BigDecimal amount) {

	static BalanceResponse from(Balance balance) {
		return new BalanceResponse(balance.getCurrency(), balance.getAmount());
	}

}
