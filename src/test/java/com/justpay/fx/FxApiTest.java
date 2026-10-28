package com.justpay.fx;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.justpay.IntegrationTest;

class FxApiTest extends IntegrationTest {

	@Test
	void should_returnSixSeededRates_when_ratesRequested() throws Exception {
		mockMvc.perform(get("/api/v1/fx/rates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(6))
				.andExpect(jsonPath("$[0].fromCurrency").value("USD"))
				.andExpect(jsonPath("$[0].toCurrency").value("MYR"))
				.andExpect(jsonPath("$[0].rate").value(4.4));
	}

}
