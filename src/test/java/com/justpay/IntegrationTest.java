package com.justpay;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justpay.account.AccountResponse;
import com.justpay.common.Currency;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

	static {
		POSTGRES.start();
	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE accounts, balances RESTART IDENTITY CASCADE");
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

}
