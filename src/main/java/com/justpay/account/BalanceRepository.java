package com.justpay.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

	List<Balance> findByAccountIdOrderById(Long accountId);

}
