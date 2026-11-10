package com.justpay.account;

import static com.justpay.common.Timestamps.now;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.justpay.common.ApiException;
import com.justpay.common.Currency;
import com.justpay.ledger.LedgerEntry;
import com.justpay.ledger.LedgerEntryResponse;
import com.justpay.ledger.LedgerRepository;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;

	public AccountService(AccountRepository accountRepository, BalanceRepository balanceRepository,
			LedgerRepository ledgerRepository) {
		this.accountRepository = accountRepository;
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
	}

	@Transactional
	public AccountResponse create(String holderName) {
		Account account = accountRepository.save(new Account(holderName, now()));
		List<Balance> balances = Arrays.stream(Currency.values())
				.map(currency -> new Balance(account.getAccountId(), currency))
				.toList();
		balanceRepository.saveAll(balances);
		return AccountResponse.from(account, balances);
	}

	@Transactional(readOnly = true)
	public AccountResponse get(Long accountId) {
		Account account = findAccount(accountId);
		return AccountResponse.from(account, balanceRepository.findByAccountIdOrderByBalanceId(accountId));
	}

	@Transactional
	public AccountResponse deposit(Long accountId, Currency currency, BigDecimal amount) {
		Account account = findAccount(accountId);
		Balance balance = balanceRepository.findForUpdateByAccountIdAndCurrency(accountId, currency)
				.orElseThrow(() -> new IllegalStateException("Account " + accountId + " has no " + currency + " balance"));
		balance.add(amount);
		ledgerRepository.save(LedgerEntry.deposit(accountId, currency, amount, now()));
		return AccountResponse.from(account, balanceRepository.findByAccountIdOrderByBalanceId(accountId));
	}

	@Transactional(readOnly = true)
	public List<LedgerEntryResponse> getLedger(Long accountId, Currency currency) {
		findAccount(accountId);
		List<LedgerEntry> entries = currency == null
				? ledgerRepository.findByAccountIdOrderByCreatedAtDescLedgerEntryIdDesc(accountId)
				: ledgerRepository.findByAccountIdAndCurrencyOrderByCreatedAtDescLedgerEntryIdDesc(accountId, currency);
		return entries.stream().map(LedgerEntryResponse::from).toList();
	}

	private Account findAccount(Long accountId) {
		return accountRepository.findById(accountId).orElseThrow(() -> accountNotFound(accountId));
	}

	public static ApiException accountNotFound(Long accountId) {
		return new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account " + accountId + " not found");
	}

}
