package com.justpay.transfer;

import java.math.BigDecimal;

import com.justpay.common.Currency;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
		@NotBlank @Size(max = 100) String paymentRequestId,
		@NotNull Long fromAccountId,
		@NotNull Currency fromCurrency,
		@NotNull Long toAccountId,
		@NotNull Currency toCurrency,
		@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
