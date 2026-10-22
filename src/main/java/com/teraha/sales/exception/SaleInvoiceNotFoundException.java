package com.teraha.sales.exception;

public class SaleInvoiceNotFoundException extends RuntimeException {
    public SaleInvoiceNotFoundException(Long id) {
        super("Sale invoice not found with id: " + id);
    }
}
