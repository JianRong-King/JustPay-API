package com.justpay.account;

import java.time.Instant;
import java.util.List;

public record AccountResponse(Long id, String holderName, Instant createdAt, List<BalanceResponse> balances) {

	static AccountResponse from(Account account, List<Balance> balances) {
		return new AccountResponse(
				account.getId(),
				account.getHolderName(),
				account.getCreatedAt(),
				balances.stream().map(BalanceResponse::from).toList());
	}

}
