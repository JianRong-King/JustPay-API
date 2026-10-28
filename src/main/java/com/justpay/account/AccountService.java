package com.justpay.account;

import static com.justpay.common.Timestamps.now;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.justpay.common.ApiException;
import com.justpay.common.Currency;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final BalanceRepository balanceRepository;

	public AccountService(AccountRepository accountRepository, BalanceRepository balanceRepository) {
		this.accountRepository = accountRepository;
		this.balanceRepository = balanceRepository;
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

	private Account findAccount(Long id) {
		return accountRepository.findById(id).orElseThrow(() -> accountNotFound(id));
	}

	public static ApiException accountNotFound(Long id) {
		return new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account " + id + " not found");
	}

}
