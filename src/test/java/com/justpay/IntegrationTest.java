package com.justpay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

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

}
