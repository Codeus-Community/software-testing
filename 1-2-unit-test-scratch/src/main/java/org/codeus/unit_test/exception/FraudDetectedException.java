package org.codeus.unit_test.exception;

public class FraudDetectedException extends RuntimeException{
    public FraudDetectedException(String message){
        super(message);
    }
}
