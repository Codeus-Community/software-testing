package org.codeus.integration_test.exception;

public class ExchangeNotFoundException extends RuntimeException {
    public ExchangeNotFoundException(Long id) {
        super("Exchange not found with id: " + id);
    }
}
