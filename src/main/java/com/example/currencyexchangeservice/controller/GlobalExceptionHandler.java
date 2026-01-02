package com.example.currencyexchangeservice.controller;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "CONFLICT");
        body.put("message", "CurrencyExchange has been modified by another request. Please reload and retry.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

}

