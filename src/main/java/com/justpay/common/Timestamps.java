package com.justpay.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

	private Timestamps() {
	}

	public static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}
