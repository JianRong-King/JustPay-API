package com.justpay.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	public ResponseEntity<TransferResponse> create(@Valid @RequestBody TransferRequest request) {
		TransferResult result = transferService.transfer(request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.transfer());
	}

	@GetMapping("/{id}")
	public TransferResponse get(@PathVariable Long id) {
		return transferService.get(id);
	}

}
