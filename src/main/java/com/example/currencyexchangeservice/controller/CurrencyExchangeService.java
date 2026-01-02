package com.example.currencyexchangeservice.controller;

import com.example.currencyexchangeservice.module.CurrencyExchange;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrencyExchangeService {
    private final CurrencyExchangeRepository currencyExchangeRepository;
    private final String CACHE_NAME = "exchangeValue";
    public CurrencyExchangeService(CurrencyExchangeRepository currencyExchangeRepository) {
        this.currencyExchangeRepository = currencyExchangeRepository;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#from + '_' + #to")
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

    public CurrencyExchange patchExchange(Long id, CurrencyExchange partialExchange) {
        return currencyExchangeRepository.findById(id)
                .map(existing -> {
                    if (partialExchange.getFrom() != null) existing.setFrom(partialExchange.getFrom());
                    if (partialExchange.getTo() != null) existing.setTo(partialExchange.getTo());
                    if (partialExchange.getConversionMultiple() != null) existing.setConversionMultiple(partialExchange.getConversionMultiple());
                    return currencyExchangeRepository.save(existing);
                })
                .orElse(null);
    }

    @CacheEvict(cacheNames = CACHE_NAME,  key = "#from + '_' + #to")
    public boolean deleteExchange(String from, String to ) {
        CurrencyExchange exchange = currencyExchangeRepository.findByFromAndTo(from, to).orElse(null);
        if (exchange != null) {
            currencyExchangeRepository.deleteById(exchange.getId());
            return true;
        }
        return false;
    }
    @CachePut(cacheNames = CACHE_NAME, key = "#currencyExchange.from + '_' + #currencyExchange.to")
    public CurrencyExchange createExchange(CurrencyExchange currencyExchange) {
        currencyExchangeRepository.save(currencyExchange);
        return currencyExchange;
    }
    @CachePut(cacheNames = CACHE_NAME, key = "#currencyExchange.from + '_' + #currencyExchange.to")
    public CurrencyExchange updateExchange(Long id, CurrencyExchange currencyExchange) {
        return currencyExchangeRepository.findById(id)
                .map(existing -> {
                    // Ensure the incoming entity carries the same ID
                    currencyExchange.setId(id);
                    // Let JPA handle optimistic locking based on @Version field
                    return currencyExchangeRepository.save(currencyExchange);
                })
                .orElse(null);
    }
}
