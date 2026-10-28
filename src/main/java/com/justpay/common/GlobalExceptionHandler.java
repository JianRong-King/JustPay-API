package com.justpay.common;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String INVALID_REQUEST = "INVALID_REQUEST";

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		return badRequest(message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		if (ex.getCause() instanceof InvalidFormatException invalid && invalid.getTargetType() == Currency.class) {
			return unsupportedCurrency(invalid.getValue());
		}
		return badRequest("Request body is missing or malformed");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		if (ex.getRequiredType() == Currency.class) {
			return unsupportedCurrency(ex.getValue());
		}
		return badRequest("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleOther(Exception ex) {
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			HttpStatusCode status = springError.getStatusCode();
			return ResponseEntity.status(status).body(new ErrorResponse("REQUEST_ERROR", ex.getMessage()));
		}
		log.error("Unexpected error", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("INTERNAL_ERROR", "Unexpected error"));
	}

	private ResponseEntity<ErrorResponse> unsupportedCurrency(Object value) {
		return ResponseEntity.badRequest().body(new ErrorResponse("UNSUPPORTED_CURRENCY",
				"Unsupported currency '" + value + "'. Allowed values: " + Arrays.toString(Currency.values())));
	}

	private ResponseEntity<ErrorResponse> badRequest(String message) {
		return ResponseEntity.badRequest().body(new ErrorResponse(INVALID_REQUEST, message));
	}

}
