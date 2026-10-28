package com.justpay.fx;

import java.math.BigDecimal;

public record Conversion(BigDecimal toAmount, BigDecimal rate) {
}
