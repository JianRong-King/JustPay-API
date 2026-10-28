package com.justpay.fx;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.justpay.common.Currency;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

	Optional<FxRate> findByFromCurrencyAndToCurrency(Currency fromCurrency, Currency toCurrency);

	List<FxRate> findAllByOrderById();

}
