package com.justpay.fx;

import java.math.BigDecimal;

import com.justpay.common.Currency;

public record FxRateResponse(Currency fromCurrency, Currency toCurrency, BigDecimal rate) {

	static FxRateResponse from(FxRate fxRate) {
		return new FxRateResponse(fxRate.getFromCurrency(), fxRate.getToCurrency(), fxRate.getRate());
	}

}
