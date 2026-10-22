package com.teraha.sales.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long id){
        super("Customer with id "+ id +" was not found");
    }
}
