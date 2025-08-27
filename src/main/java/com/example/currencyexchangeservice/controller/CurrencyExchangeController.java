package com.example.currencyexchangeservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class CurrencyExchangeController {
    @Autowired
    private Environment environment;
    private CurrencyExchangeService currencyExchangeService;

    public CurrencyExchangeController(CurrencyExchangeService currencyExchangeService) {
        this.currencyExchangeService = currencyExchangeService;
    }
@GetMapping("/currency-exchange/from/{from}/to/{to}")
    public ResponseEntity<?> retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {

    //CurrencyExchange currencyExchange = CurrencyExchange.builder().id(1000L).from(from).to(to).conversionMultiple(BigDecimal.valueOf(50)).build();
//    String port = environment.getProperty("local.server.port");
//    currencyExchange.setEnvironment(port);
    CurrencyExchange currencyExchange = currencyExchangeService.retrieveExchangeValue(from, to);
    if(currencyExchange != null) {
        currencyExchange.setEnvironment(environment.getProperty("local.server.port"));
        return ResponseEntity.ok().body(currencyExchange);
    }
    return new ResponseEntity<>("Conversion record not found",HttpStatus.NOT_FOUND);
}

}
