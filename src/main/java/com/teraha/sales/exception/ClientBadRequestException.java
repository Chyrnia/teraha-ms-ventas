package com.teraha.sales.exception;

public class ClientBadRequestException extends RuntimeException {
    public ClientBadRequestException(String message) {
        super(message);
    }
}
