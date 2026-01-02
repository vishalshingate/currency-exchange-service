package com.example.currencyexchangeservice.controller;

import com.example.currencyexchangeservice.module.CurrencyExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchange, Long> {

    public Optional<CurrencyExchange> findByFromAndTo(String from, String to);

}
