package com.loopers.infrastructure.pg;

public class PgUnavailableException extends RuntimeException {
    public PgUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
