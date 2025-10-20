package com.sporty.behometask.exception;

/**
 * Exception thrown when an invalid ICAO code format is provided.
 */
public class InvalidIcaoCodeException extends RuntimeException {
    public InvalidIcaoCodeException(String message) {
        super(message);
    }
}


