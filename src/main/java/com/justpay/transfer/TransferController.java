package com.justpay.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransferResponse create(@Valid @RequestBody TransferRequest request) {
		return transferService.transfer(request);
	}

	@GetMapping("/{id}")
	public TransferResponse get(@PathVariable Long id) {
		return transferService.get(id);
	}

}
