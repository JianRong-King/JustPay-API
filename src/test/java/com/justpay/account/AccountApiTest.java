package com.justpay.account;

import static com.justpay.common.Currency.MYR;
import static com.justpay.common.Currency.SGD;
import static com.justpay.common.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.justpay.IntegrationTest;

class AccountApiTest extends IntegrationTest {

	@Test
	void should_createThreeZeroBalances_when_accountCreated() throws Exception {
		AccountResponse created = createAccount("Alice");

		assertThat(created.holderName()).isEqualTo("Alice");
		assertThat(created.balances()).extracting(BalanceResponse::currency).containsExactly(MYR, USD, SGD);
		assertThat(created.balances()).allSatisfy(balance -> assertThat(balance.amount()).isEqualByComparingTo("0"));

		String body = mockMvc.perform(get("/api/v1/accounts/" + created.id()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(objectMapper.readValue(body, AccountResponse.class)).isEqualTo(created);
	}

	@Test
	void should_return404_when_accountDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/v1/accounts/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void should_return400_when_holderNameIsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"holderName\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

}
