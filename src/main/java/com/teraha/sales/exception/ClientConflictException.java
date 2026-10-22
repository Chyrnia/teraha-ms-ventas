package com.teraha.sales.exception;

public class ClientConflictException extends RuntimeException {
    public ClientConflictException(String code) {
        super("The Customer with code " + code + " already exists");;
    }
}
