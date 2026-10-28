package com.justpay.fx;

import static com.justpay.common.Currency.MYR;
import static com.justpay.common.Currency.SGD;
import static com.justpay.common.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.justpay.IntegrationTest;
import com.justpay.common.Currency;

class FxServiceTest extends IntegrationTest {

	@Autowired
	private FxService fxService;

	@Test
	void should_multiplyByRateAndRoundToTwoDecimals_when_currenciesDiffer() {
		Conversion conversion = fxService.convert(new BigDecimal("10.01"), USD, MYR);

		assertThat(conversion.toAmount()).isEqualByComparingTo("44.04");
		assertThat(conversion.rate()).isEqualByComparingTo("4.400000");
	}

	@Test
	void should_roundHalfToEven_when_resultEndsInExactlyFive() {
		assertThat(fxService.convert(new BigDecimal("0.15"), MYR, SGD).toAmount()).isEqualByComparingTo("0.04");
		assertThat(fxService.convert(new BigDecimal("0.25"), MYR, SGD).toAmount()).isEqualByComparingTo("0.08");
	}

	@Test
	void should_keepAmountWithRateOne_when_currenciesEqual() {
		Conversion conversion = fxService.convert(new BigDecimal("12.34"), SGD, SGD);

		assertThat(conversion.toAmount()).isEqualByComparingTo("12.34");
		assertThat(conversion.rate()).isEqualByComparingTo("1");
	}

	@Test
	void should_neverGainMoney_when_convertingThereAndBack() {
		List<String> amounts = List.of("0.01", "1.00", "100.00", "12345.67");
		for (Currency from : Currency.values()) {
			for (Currency to : Currency.values()) {
				for (String amount : amounts) {
					BigDecimal original = new BigDecimal(amount);
					BigDecimal there = fxService.convert(original, from, to).toAmount();
					BigDecimal back = fxService.convert(there, to, from).toAmount();
					assertThat(back).as("%s %s -> %s -> %s", amount, from, to, from).isLessThanOrEqualTo(original);
				}
			}
		}
	}

	@Test
	void should_loseAboutThreePercent_when_usdConvertedToMyrAndBack() {
		BigDecimal myr = fxService.convert(new BigDecimal("100.00"), USD, MYR).toAmount();
		BigDecimal usd = fxService.convert(myr, MYR, USD).toAmount();

		assertThat(myr).isEqualByComparingTo("440.00");
		assertThat(usd).isEqualByComparingTo("96.80");
	}

}
