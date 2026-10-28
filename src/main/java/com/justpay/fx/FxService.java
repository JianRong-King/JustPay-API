package com.justpay.fx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.justpay.common.Currency;

@Service
public class FxService {

	private static final BigDecimal SAME_CURRENCY_RATE = BigDecimal.ONE.setScale(6);

	private final FxRateRepository fxRateRepository;

	public FxService(FxRateRepository fxRateRepository) {
		this.fxRateRepository = fxRateRepository;
	}

	@Transactional(readOnly = true)
	public Conversion convert(BigDecimal amount, Currency from, Currency to) {
		if (from == to) {
			return new Conversion(amount, SAME_CURRENCY_RATE);
		}
		BigDecimal rate = fxRateRepository.findByFromCurrencyAndToCurrency(from, to)
				.orElseThrow(() -> new IllegalStateException("No FX rate from " + from + " to " + to))
				.getRate();
		BigDecimal toAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
		return new Conversion(toAmount, rate);
	}

	@Transactional(readOnly = true)
	public List<FxRateResponse> listRates() {
		return fxRateRepository.findAllByOrderById().stream().map(FxRateResponse::from).toList();
	}

}
