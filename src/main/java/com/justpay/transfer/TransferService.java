package com.justpay.transfer;

import static com.justpay.common.Timestamps.now;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.justpay.account.AccountRepository;
import com.justpay.account.AccountService;
import com.justpay.account.Balance;
import com.justpay.account.BalanceRepository;
import com.justpay.common.ApiException;
import com.justpay.common.Currency;
import com.justpay.fx.Conversion;
import com.justpay.fx.FxService;
import com.justpay.ledger.LedgerEntry;
import com.justpay.ledger.LedgerRepository;

@Service
public class TransferService {

	private final TransferRepository transferRepository;
	private final AccountRepository accountRepository;
	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final FxService fxService;

	public TransferService(TransferRepository transferRepository, AccountRepository accountRepository,
			BalanceRepository balanceRepository, LedgerRepository ledgerRepository, FxService fxService) {
		this.transferRepository = transferRepository;
		this.accountRepository = accountRepository;
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.fxService = fxService;
	}

	@Transactional
	public TransferResponse transfer(TransferRequest request) {
		requireAccountExists(request.fromAccountId());
		requireAccountExists(request.toAccountId());
		requireDifferentBalances(request);

		BigDecimal amount = request.amount().setScale(2);
		Conversion conversion = fxService.convert(amount, request.fromCurrency(), request.toCurrency());
		requireNonZero(conversion.toAmount());

		LockedBalances balances = lockBalances(request);

		requireEnoughMoney(balances.from(), amount);

		balances.from().subtract(amount);
		balances.to().add(conversion.toAmount());

		Instant now = now();
		Transfer transfer = transferRepository.save(new Transfer(request.paymentRequestId(),
				request.fromAccountId(), request.fromCurrency(), amount,
				request.toAccountId(), request.toCurrency(), conversion.toAmount(),
				conversion.rate(), now));
		ledgerRepository.save(LedgerEntry.transferOut(request.fromAccountId(), request.fromCurrency(),
				amount, transfer.getId(), now));
		ledgerRepository.save(LedgerEntry.transferIn(request.toAccountId(), request.toCurrency(),
				conversion.toAmount(), transfer.getId(), now));

		return TransferResponse.from(transfer);
	}

	@Transactional(readOnly = true)
	public TransferResponse get(Long id) {
		return transferRepository.findById(id)
				.map(TransferResponse::from)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND",
						"Transfer " + id + " not found"));
	}

	private void requireAccountExists(Long accountId) {
		if (!accountRepository.existsById(accountId)) {
			throw AccountService.accountNotFound(accountId);
		}
	}

	private static void requireDifferentBalances(TransferRequest request) {
		if (request.fromAccountId().equals(request.toAccountId())
				&& request.fromCurrency() == request.toCurrency()) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SAME_BALANCE",
					"Cannot transfer from a balance to itself");
		}
	}

	private static void requireNonZero(BigDecimal toAmount) {
		if (toAmount.signum() == 0) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AMOUNT_TOO_SMALL",
					"Converted amount rounds to 0.00");
		}
	}

	private static void requireEnoughMoney(Balance from, BigDecimal amount) {
		if (from.getAmount().compareTo(amount) < 0) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE",
					"Balance " + from.getCurrency() + " has " + from.getAmount() + ", needs " + amount);
		}
	}

	private LockedBalances lockBalances(TransferRequest request) {
		Long fromId = findBalanceId(request.fromAccountId(), request.fromCurrency());
		Long toId = findBalanceId(request.toAccountId(), request.toCurrency());
		if (fromId < toId) {
			Balance from = lockBalance(fromId);
			Balance to = lockBalance(toId);
			return new LockedBalances(from, to);
		}
		Balance to = lockBalance(toId);
		Balance from = lockBalance(fromId);
		return new LockedBalances(from, to);
	}

	private Long findBalanceId(Long accountId, Currency currency) {
		return balanceRepository.findIdByAccountIdAndCurrency(accountId, currency)
				.orElseThrow(() -> new IllegalStateException(
						"Account " + accountId + " has no " + currency + " balance"));
	}

	private Balance lockBalance(Long balanceId) {
		return balanceRepository.findForUpdateById(balanceId)
				.orElseThrow(() -> new IllegalStateException("Balance " + balanceId + " not found"));
	}

	private record LockedBalances(Balance from, Balance to) {
	}

}
