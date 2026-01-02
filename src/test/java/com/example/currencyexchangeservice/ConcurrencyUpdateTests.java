package com.example.currencyexchangeservice;

import com.example.currencyexchangeservice.module.CurrencyExchange;
import com.example.currencyexchangeservice.controller.CurrencyExchangeRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;

@SpringBootTest
class ConcurrencyUpdateTests {

    @Autowired
    private CurrencyExchangeRepository repository;

    @Test
    void concurrentUpdate_shouldThrowOptimisticLockExceptionOnSecondSave() {
        CurrencyExchange exchange = new CurrencyExchange();
        exchange.setId(1000L);
        exchange.setFrom("USD");
        exchange.setTo("INR");
        exchange.setConversionMultiple(BigDecimal.TEN);
        exchange = repository.saveAndFlush(exchange);

        // Detach and reload to simulate two different concurrent transactions
        CurrencyExchange firstUpdate = repository.findById(exchange.getId()).orElseThrow();
        CurrencyExchange secondUpdate = repository.findById(exchange.getId()).orElseThrow();

        firstUpdate.setConversionMultiple(new BigDecimal("20"));
        repository.saveAndFlush(firstUpdate);

        secondUpdate.setConversionMultiple(new BigDecimal("30"));

        Assertions.assertThrows(OptimisticLockingFailureException.class, () -> repository.saveAndFlush(secondUpdate));
    }
}
