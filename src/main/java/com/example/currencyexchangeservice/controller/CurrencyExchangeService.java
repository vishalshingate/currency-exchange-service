package com.example.currencyexchangeservice.controller;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrencyExchangeService {
    private final CurrencyExchangeRepository currencyExchangeRepository;

    public CurrencyExchangeService(CurrencyExchangeRepository currencyExchangeRepository) {
        this.currencyExchangeRepository = currencyExchangeRepository;
    }

    public CurrencyExchange retrieveExchangeValue(String from, String to) {

        Optional<CurrencyExchange> exchange =  currencyExchangeRepository.findByFromAndTo(from, to);
        if (exchange.isPresent()) {
            return exchange.get();
        }
        else
        {
            return null;
        }
    }
}
