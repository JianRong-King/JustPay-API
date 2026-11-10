package com.justpay.ledger;

import java.math.BigDecimal;
import java.time.Instant;

import com.justpay.common.Currency;

public record LedgerEntryResponse(Long ledgerEntryId, Currency currency, BigDecimal amount, LedgerEntryType type,
		Long transferId, Instant createdAt) {

	public static LedgerEntryResponse from(LedgerEntry entry) {
		return new LedgerEntryResponse(entry.getLedgerEntryId(), entry.getCurrency(), entry.getAmount(), entry.getType(),
				entry.getTransferId(), entry.getCreatedAt());
	}

}
