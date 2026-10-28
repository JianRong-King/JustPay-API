package com.justpay.account;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.justpay.common.Currency;
import com.justpay.ledger.LedgerEntryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
		return accountService.create(request.holderName());
	}

	@GetMapping("/{id}")
	public AccountResponse get(@PathVariable Long id) {
		return accountService.get(id);
	}

	@PostMapping("/{id}/deposits")
	public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
		return accountService.deposit(id, request.currency(), request.amount());
	}

	@GetMapping("/{id}/ledger")
	public List<LedgerEntryResponse> ledger(@PathVariable Long id,
			@RequestParam(required = false) Currency currency) {
		return accountService.getLedger(id, currency);
	}

}
