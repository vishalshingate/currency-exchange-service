package com.example.currencyexchangeservice.controller;

import com.example.currencyexchangeservice.module.CurrencyExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CurrencyExchangeController {
    @Value("${server.port}")
    private String port;
    @Autowired
    private Environment environment;

    private final CurrencyExchangeService currencyExchangeService;

    private final Logger logger = LoggerFactory.getLogger(CurrencyExchangeController.class);

    public CurrencyExchangeController(CurrencyExchangeService currencyExchangeService) {
        this.currencyExchangeService = currencyExchangeService;
    }
@GetMapping("/currency-exchange/from/{from}/to/{to}")
    public ResponseEntity<?> retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {

    //CurrencyExchange currencyExchange = CurrencyExchange.builder().id(1000L).from(from).to(to).conversionMultiple(BigDecimal.valueOf(50)).build();
//    String port = environment.getProperty("local.server.port");
//    currencyExchange.setEnvironment(port);
    logger.info("retrieveExchangeValue called with {} to {} with port : {}", from, to, port);
    CurrencyExchange currencyExchange = currencyExchangeService.retrieveExchangeValue(from, to);
    if(currencyExchange != null) {
        currencyExchange.setEnvironment(environment.getProperty("local.server.port"));
        return ResponseEntity.ok().body(currencyExchange);
    }
    return new ResponseEntity<>("Conversion record not found",HttpStatus.NOT_FOUND);
}

    @PostMapping("/currency-exchange")
    public ResponseEntity<?> createExchange(@RequestBody CurrencyExchange currencyExchange) {
        CurrencyExchange created = currencyExchangeService.createExchange(currencyExchange);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/currency-exchange/{id}")
    public ResponseEntity<?> updateExchange(@PathVariable Long id, @RequestBody CurrencyExchange currencyExchange) {
        CurrencyExchange updated = currencyExchangeService.updateExchange(id, currencyExchange);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CurrencyExchange not found");
    }

    @PatchMapping("/currency-exchange/{id}")
    public ResponseEntity<?> patchExchange(@PathVariable Long id, @RequestBody CurrencyExchange currencyExchange) {
        CurrencyExchange patched = currencyExchangeService.patchExchange(id, currencyExchange);
        if (patched != null) {
            return ResponseEntity.ok(patched);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CurrencyExchange not found");
    }

    @DeleteMapping("/currency-exchange/from/{from}/to/{to}")
    public ResponseEntity<?> deleteExchange(@PathVariable String from, @PathVariable String to) {
        boolean deleted = currencyExchangeService.deleteExchange(from, to);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK).body("CurrencyExchange deleted");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CurrencyExchange not found");
    }

}
