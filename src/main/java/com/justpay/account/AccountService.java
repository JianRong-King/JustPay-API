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
				.map(currency -> new Balance(account.getId(), currency))
				.toList();
		balanceRepository.saveAll(balances);
		return AccountResponse.from(account, balances);
	}

	@Transactional(readOnly = true)
	public AccountResponse get(Long id) {
		Account account = findAccount(id);
		return AccountResponse.from(account, balanceRepository.findByAccountIdOrderById(id));
	}

	@Transactional
	public AccountResponse deposit(Long id, Currency currency, BigDecimal amount) {
		Account account = findAccount(id);
		Balance balance = balanceRepository.findForUpdateByAccountIdAndCurrency(id, currency)
				.orElseThrow(() -> new IllegalStateException("Account " + id + " has no " + currency + " balance"));
		balance.add(amount);
		ledgerRepository.save(LedgerEntry.deposit(id, currency, amount, now()));
		return AccountResponse.from(account, balanceRepository.findByAccountIdOrderById(id));
	}

	@Transactional(readOnly = true)
	public List<LedgerEntryResponse> getLedger(Long id, Currency currency) {
		findAccount(id);
		List<LedgerEntry> entries = currency == null
				? ledgerRepository.findByAccountIdOrderByCreatedAtDescIdDesc(id)
				: ledgerRepository.findByAccountIdAndCurrencyOrderByCreatedAtDescIdDesc(id, currency);
		return entries.stream().map(LedgerEntryResponse::from).toList();
	}

	private Account findAccount(Long id) {
		return accountRepository.findById(id).orElseThrow(() -> accountNotFound(id));
	}

	public static ApiException accountNotFound(Long id) {
		return new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account " + id + " not found");
	}

}
