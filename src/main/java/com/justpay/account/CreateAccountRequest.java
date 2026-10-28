package com.justpay.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(@NotBlank @Size(max = 100) String holderName) {
}
