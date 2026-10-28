package com.justpay.transfer;

import static com.justpay.common.Currency.MYR;
import static com.justpay.common.Currency.SGD;
import static com.justpay.common.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.justpay.IntegrationTest;
import com.justpay.account.AccountResponse;
import com.justpay.ledger.LedgerEntryResponse;
import com.justpay.ledger.LedgerEntryType;

class TransferApiTest extends IntegrationTest {

	@Test
	void should_updateBothBalancesAndWriteTwoLedgerEntries_when_sameCurrencyTransfer() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");

		TransferResponse transfer = transfer("pr-1", alice, "USD", bob, "USD", "30.00");

		assertThat(transfer.toAmount()).isEqualByComparingTo("30.00");
		assertThat(transfer.rate()).isEqualByComparingTo("1");
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("70.00");
		assertThat(amountOf(getAccount(bob), USD)).isEqualByComparingTo("30.00");

		LedgerEntryResponse out = getLedger(alice, "").get(0);
		assertThat(out.type()).isEqualTo(LedgerEntryType.TRANSFER_OUT);
		assertThat(out.amount()).isEqualByComparingTo("-30.00");
		assertThat(out.transferId()).isEqualTo(transfer.id());

		List<LedgerEntryResponse> bobLedger = getLedger(bob, "");
		assertThat(bobLedger).hasSize(1);
		assertThat(bobLedger.get(0).type()).isEqualTo(LedgerEntryType.TRANSFER_IN);
		assertThat(bobLedger.get(0).amount()).isEqualByComparingTo("30.00");
		assertThat(bobLedger.get(0).transferId()).isEqualTo(transfer.id());
	}

	@Test
	void should_creditAmountTimesRateRoundedAndStoreRate_when_crossCurrencyTransfer() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");

		TransferResponse transfer = transfer("pr-1", alice, "USD", bob, "MYR", "10.01");

		assertThat(transfer.fromAmount()).isEqualByComparingTo("10.01");
		assertThat(transfer.toAmount()).isEqualByComparingTo("44.04");
		assertThat(transfer.rate()).isEqualByComparingTo("4.400000");
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("89.99");
		assertThat(amountOf(getAccount(bob), MYR)).isEqualByComparingTo("44.04");
		assertThat(getTransfer(transfer.id())).isEqualTo(transfer);
	}

	@Test
	void should_updateBothBalancesOfSameAccount_when_convertingUsdToMyr() throws Exception {
		Long alice = createAccount("Alice").id();
		deposit(alice, USD, "100.00");

		transfer("pr-1", alice, "USD", alice, "MYR", "25.00");

		AccountResponse account = getAccount(alice);
		assertThat(amountOf(account, USD)).isEqualByComparingTo("75.00");
		assertThat(amountOf(account, MYR)).isEqualByComparingTo("110.00");
		assertThat(amountOf(account, SGD)).isEqualByComparingTo("0");
	}

	@Test
	void should_return422SameBalance_when_sameAccountAndSameCurrency() throws Exception {
		Long alice = createAccount("Alice").id();
		deposit(alice, USD, "100.00");

		postTransfer(transferJson("pr-1", alice, "USD", alice, "USD", "10.00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("SAME_BALANCE"));

		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("100.00");
	}

	@Test
	void should_return422AndChangeNoBalances_when_insufficientBalance() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "10.00");

		postTransfer(transferJson("pr-1", alice, "USD", bob, "MYR", "10.01"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("10.00");
		assertThat(amountOf(getAccount(bob), MYR)).isEqualByComparingTo("0");
		assertThat(getLedger(alice, "")).hasSize(1);
		assertThat(getLedger(bob, "")).isEmpty();
	}

	@Test
	void should_allowTransfer_when_amountEqualsWholeBalance() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, SGD, "10.00");

		transfer("pr-1", alice, "SGD", bob, "SGD", "10.00");

		assertThat(amountOf(getAccount(alice), SGD)).isEqualByComparingTo("0");
		assertThat(amountOf(getAccount(bob), SGD)).isEqualByComparingTo("10.00");
	}

	@Test
	void should_return422AmountTooSmall_when_convertedAmountRoundsToZero() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, MYR, "1.00");

		postTransfer(transferJson("pr-1", alice, "MYR", bob, "USD", "0.01"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("AMOUNT_TOO_SMALL"));

		assertThat(amountOf(getAccount(alice), MYR)).isEqualByComparingTo("1.00");
	}

	@Test
	void should_return404_when_transferAccountUnknown() throws Exception {
		Long alice = createAccount("Alice").id();
		deposit(alice, USD, "100.00");

		postTransfer(transferJson("pr-1", alice, "USD", 999L, "USD", "10.00"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));

		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("100.00");
	}

	@Test
	void should_return400_when_transferCurrencyUnsupported() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();

		postTransfer(transferJson("pr-1", alice, "USD", bob, "EUR", "10.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
	}

	@Test
	void should_return400_when_paymentRequestIdMissing() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();

		postTransfer(transferJson("", alice, "USD", bob, "USD", "10.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void should_return404_when_transferUnknown() throws Exception {
		mockMvc.perform(get("/api/v1/transfers/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
	}

	private TransferResponse transfer(String paymentRequestId, Long fromAccountId, String fromCurrency,
			Long toAccountId, String toCurrency, String amount) throws Exception {
		String body = postTransfer(transferJson(paymentRequestId, fromAccountId, fromCurrency, toAccountId,
				toCurrency, amount))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, TransferResponse.class);
	}

	private TransferResponse getTransfer(Long id) throws Exception {
		String body = mockMvc.perform(get("/api/v1/transfers/" + id))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, TransferResponse.class);
	}

}
