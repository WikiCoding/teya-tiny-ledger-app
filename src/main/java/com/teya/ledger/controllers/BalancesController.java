package com.teya.ledger.controllers;

import com.teya.ledger.dtos.BalanceResponse;
import com.teya.ledger.services.BalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/balances")
public class BalancesController {
    private final BalanceService balanceService;

    @Autowired
    public BalancesController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public ResponseEntity<BalanceResponse> getLedgerBalance() {
        return ResponseEntity.ok(new BalanceResponse(balanceService.getLedgerBalance()));
    }
}
