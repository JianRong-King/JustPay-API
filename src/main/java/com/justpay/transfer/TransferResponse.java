package com.justpay.transfer;

import java.math.BigDecimal;
import java.time.Instant;

import com.justpay.common.Currency;

public record TransferResponse(Long transferId, String paymentRequestId, Long fromAccountId, Currency fromCurrency,
		BigDecimal fromAmount, Long toAccountId, Currency toCurrency, BigDecimal toAmount, BigDecimal rate,
		Instant createdAt) {

	static TransferResponse from(Transfer transfer) {
		return new TransferResponse(transfer.getTransferId(), transfer.getPaymentRequestId(), transfer.getFromAccountId(),
				transfer.getFromCurrency(), transfer.getFromAmount(), transfer.getToAccountId(),
				transfer.getToCurrency(), transfer.getToAmount(), transfer.getRate(), transfer.getCreatedAt());
	}

}
