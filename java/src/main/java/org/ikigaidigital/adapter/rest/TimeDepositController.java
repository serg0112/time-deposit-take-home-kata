package org.ikigaidigital.adapter.rest;

import lombok.RequiredArgsConstructor;
import org.ikigaidigital.adapter.rest.response.DepositDetails;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/time-deposits")
@CrossOrigin(origins = {"http://localhost:8081", "http://127.0.0.1:8081"})
public class TimeDepositController {
    private final TimeDepositService service;

    @GetMapping
    public List<DepositDetails> findAll() {
        return service.findAll();
    }

    @PostMapping("/update-balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateBalances();
    }
}
