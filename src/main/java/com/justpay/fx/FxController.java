package com.justpay.fx;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fx")
public class FxController {

	private final FxService fxService;

	public FxController(FxService fxService) {
		this.fxService = fxService;
	}

	@GetMapping("/rates")
	public List<FxRateResponse> rates() {
		return fxService.listRates();
	}

}
