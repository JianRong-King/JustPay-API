package com.justpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justpay.account.AccountResponse;
import com.justpay.common.Currency;
import com.justpay.ledger.LedgerEntryResponse;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

	static {
		POSTGRES.start();
	}

	private static final String BALANCES_NOT_MATCHING_LEDGER = """
			SELECT b.account_id, b.currency, b.amount, COALESCE(SUM(l.amount), 0) AS ledger_sum
			FROM balances b
			LEFT JOIN ledger_entries l ON l.account_id = b.account_id AND l.currency = b.currency
			GROUP BY b.id, b.account_id, b.currency, b.amount
			HAVING b.amount <> COALESCE(SUM(l.amount), 0)
			""";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE accounts, balances, ledger_entries, transfers RESTART IDENTITY CASCADE");
	}

	@AfterEach
	void verifyEveryBalanceMatchesItsLedger() {
		List<Map<String, Object>> mismatches = jdbcTemplate.queryForList(BALANCES_NOT_MATCHING_LEDGER);
		assertThat(mismatches).as("balances that differ from the sum of their ledger entries").isEmpty();
	}

	protected AccountResponse createAccount(String holderName) throws Exception {
		String body = mockMvc.perform(post("/api/v1/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"holderName\": \"" + holderName + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AccountResponse.class);
	}

	protected AccountResponse getAccount(Long accountId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/accounts/" + accountId))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AccountResponse.class);
	}

	protected static BigDecimal amountOf(AccountResponse account, Currency currency) {
		return account.balances().stream()
				.filter(balance -> balance.currency() == currency)
				.findFirst()
				.orElseThrow()
				.amount();
	}

	protected AccountResponse deposit(Long accountId, Currency currency, String amount) throws Exception {
		String body = mockMvc.perform(post("/api/v1/accounts/" + accountId + "/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content(depositJson(currency.name(), amount)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, AccountResponse.class);
	}

	protected List<LedgerEntryResponse> getLedger(Long accountId, String query) throws Exception {
		String body = mockMvc.perform(get("/api/v1/accounts/" + accountId + "/ledger" + query))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, new TypeReference<>() {
		});
	}

	protected static String depositJson(String currency, String amount) {
		return "{\"currency\": \"" + currency + "\", \"amount\": " + amount + "}";
	}

	protected ResultActions postTransfer(String json) throws Exception {
		return mockMvc.perform(post("/api/v1/transfers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	protected static String transferJson(String paymentRequestId, Long fromAccountId, String fromCurrency,
			Long toAccountId, String toCurrency, String amount) {
		return """
				{"paymentRequestId": "%s", "fromAccountId": %d, "fromCurrency": "%s",
				 "toAccountId": %d, "toCurrency": "%s", "amount": %s}
				""".formatted(paymentRequestId, fromAccountId, fromCurrency, toAccountId, toCurrency, amount);
	}

}
