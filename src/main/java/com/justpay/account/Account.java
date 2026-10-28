package com.justpay.account;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String holderName;

	private Instant createdAt;

	protected Account() {
	}

	public Account(String holderName, Instant createdAt) {
		this.holderName = holderName;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getHolderName() {
		return holderName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
