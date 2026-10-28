package com.justpay.account;

import java.math.BigDecimal;

import com.justpay.common.Currency;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
		@NotNull Currency currency,
		@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
