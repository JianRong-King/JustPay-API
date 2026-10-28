package com.justpay.account;

import static com.justpay.common.Currency.MYR;
import static com.justpay.common.Currency.SGD;
import static com.justpay.common.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.justpay.IntegrationTest;
import com.justpay.ledger.LedgerEntryResponse;
import com.justpay.ledger.LedgerEntryType;

class DepositApiTest extends IntegrationTest {

	@Test
	void should_increaseOnlyUsdBalanceAndWriteOneLedgerEntry_when_usdDeposited() throws Exception {
		Long accountId = createAccount("Alice").id();

		AccountResponse account = deposit(accountId, USD, "100.50");

		assertThat(amountOf(account, MYR)).isEqualByComparingTo("0");
		assertThat(amountOf(account, USD)).isEqualByComparingTo("100.50");
		assertThat(amountOf(account, SGD)).isEqualByComparingTo("0");

		List<LedgerEntryResponse> ledger = getLedger(accountId, "");
		assertThat(ledger).hasSize(1);
		LedgerEntryResponse entry = ledger.get(0);
		assertThat(entry.currency()).isEqualTo(USD);
		assertThat(entry.amount()).isEqualByComparingTo("100.50");
		assertThat(entry.type()).isEqualTo(LedgerEntryType.DEPOSIT);
		assertThat(entry.transferId()).isNull();
	}

	@Test
	void should_return400_when_depositCurrencyUnsupported() throws Exception {
		Long accountId = createAccount("Alice").id();

		mockMvc.perform(post("/api/v1/accounts/" + accountId + "/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(depositJson("EUR", "10.00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));

		assertThat(getLedger(accountId, "")).isEmpty();
	}

	@Test
	void should_return400_when_depositAmountIsZero() throws Exception {
		Long accountId = createAccount("Alice").id();

		postDeposit(accountId, depositJson("USD", "0"));
	}

	@Test
	void should_return400_when_depositAmountIsNegative() throws Exception {
		Long accountId = createAccount("Alice").id();

		postDeposit(accountId, depositJson("USD", "-5.00"));
	}

	@Test
	void should_return400_when_depositAmountHasMoreThanTwoDecimals() throws Exception {
		Long accountId = createAccount("Alice").id();

		postDeposit(accountId, depositJson("USD", "1.234"));
	}

	@Test
	void should_return404_when_depositToUnknownAccount() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/999/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(depositJson("USD", "10.00")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void should_returnNewestFirstFilteredEntries_when_ledgerCurrencyGiven() throws Exception {
		Long accountId = createAccount("Alice").id();
		deposit(accountId, USD, "10.00");
		deposit(accountId, MYR, "20.00");
		deposit(accountId, USD, "5.00");

		List<LedgerEntryResponse> usdEntries = getLedger(accountId, "?currency=USD");

		assertThat(usdEntries).extracting(LedgerEntryResponse::currency).containsOnly(USD);
		assertThat(usdEntries).extracting(LedgerEntryResponse::amount)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("5.00"), new BigDecimal("10.00"));
		assertThat(getLedger(accountId, "")).hasSize(3);
	}

	@Test
	void should_return400_when_ledgerCurrencyUnsupported() throws Exception {
		Long accountId = createAccount("Alice").id();

		mockMvc.perform(get("/api/v1/accounts/" + accountId + "/ledger?currency=EUR"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
	}

	@Test
	void should_return404_when_ledgerOfUnknownAccount() throws Exception {
		mockMvc.perform(get("/api/v1/accounts/999/ledger"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	private void postDeposit(Long accountId, String json) throws Exception {
		mockMvc.perform(post("/api/v1/accounts/" + accountId + "/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

}
