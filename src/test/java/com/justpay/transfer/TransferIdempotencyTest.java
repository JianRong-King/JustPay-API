package com.justpay.transfer;

import static com.justpay.common.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.justpay.IntegrationTest;

class TransferIdempotencyTest extends IntegrationTest {

	@Test
	void should_createOneTransferAndReturnSameResponse_when_samePaymentRequestIdSentTwice() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");
		String json = transferJson("pr-1", alice, "USD", bob, "USD", "30.00");

		String first = postTransfer(json).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String second = postTransfer(json).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(objectMapper.readTree(second)).isEqualTo(objectMapper.readTree(first));
		assertThat(transferCount()).isEqualTo(1);
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("70.00");
		assertThat(amountOf(getAccount(bob), USD)).isEqualByComparingTo("30.00");
	}

	@Test
	void should_replay_when_sameAmountSentWithDifferentScale() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");

		postTransfer(transferJson("pr-1", alice, "USD", bob, "USD", "30.00")).andExpect(status().isCreated());
		postTransfer(transferJson("pr-1", alice, "USD", bob, "USD", "30")).andExpect(status().isOk());

		assertThat(transferCount()).isEqualTo(1);
	}

	@Test
	void should_return409_when_samePaymentRequestIdWithDifferentDetails() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");
		postTransfer(transferJson("pr-1", alice, "USD", bob, "USD", "30.00")).andExpect(status().isCreated());

		List<String> conflicting = List.of(
				transferJson("pr-1", alice, "USD", bob, "USD", "31.00"),
				transferJson("pr-1", alice, "USD", bob, "MYR", "30.00"),
				transferJson("pr-1", bob, "USD", alice, "USD", "30.00"),
				transferJson("pr-1", alice, "USD", alice, "SGD", "30.00"));
		for (String json : conflicting) {
			postTransfer(json)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.code").value("PAYMENT_REQUEST_ID_CONFLICT"));
		}

		assertThat(transferCount()).isEqualTo(1);
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("70.00");
	}

	@Test
	void should_replayInsteadOf422_when_originalTransferSpentWholeBalance() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "30.00");
		String json = transferJson("pr-1", alice, "USD", bob, "USD", "30.00");

		postTransfer(json).andExpect(status().isCreated());
		postTransfer(json).andExpect(status().isOk());

		assertThat(transferCount()).isEqualTo(1);
	}

	@Test
	void should_createExactlyOneTransfer_when_sameRequestSentConcurrently() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "100.00");
		String json = transferJson("pr-1", alice, "USD", bob, "USD", "30.00");

		List<Integer> statuses = postConcurrently(List.of(json, json, json, json, json));

		assertThat(statuses).containsOnlyOnce(201);
		assertThat(statuses).filteredOn(code -> code == 200).hasSize(4);
		assertThat(transferCount()).isEqualTo(1);
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("70.00");
	}

	@Test
	void should_notDeadlockAndKeepTotals_when_oppositeTransfersRunConcurrently() throws Exception {
		Long alice = createAccount("Alice").id();
		Long bob = createAccount("Bob").id();
		deposit(alice, USD, "1000.00");
		deposit(bob, USD, "1000.00");

		List<String> requests = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			requests.add(i % 2 == 0
					? transferJson("a-to-b-" + i, alice, "USD", bob, "USD", "10.00")
					: transferJson("b-to-a-" + i, bob, "USD", alice, "USD", "10.00"));
		}

		List<Integer> statuses = postConcurrently(requests);

		assertThat(statuses).containsOnly(201);
		assertThat(transferCount()).isEqualTo(20);
		assertThat(amountOf(getAccount(alice), USD)).isEqualByComparingTo("1000.00");
		assertThat(amountOf(getAccount(bob), USD)).isEqualByComparingTo("1000.00");
	}

	private Integer transferCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers", Integer.class);
	}

	private List<Integer> postConcurrently(List<String> bodies) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(bodies.size());
		CountDownLatch startSignal = new CountDownLatch(1);
		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (String json : bodies) {
				futures.add(executor.submit(() -> {
					startSignal.await();
					return postTransfer(json).andReturn().getResponse().getStatus();
				}));
			}
			startSignal.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> future : futures) {
				statuses.add(future.get(30, TimeUnit.SECONDS));
			}
			return statuses;
		} finally {
			executor.shutdownNow();
		}
	}

}
