package com.teraha.sales.exception;

public class ClientConflictException extends RuntimeException {
    public ClientConflictException(String document) {
        super("The Customer with document " + document + " already exists");;
    }
}
