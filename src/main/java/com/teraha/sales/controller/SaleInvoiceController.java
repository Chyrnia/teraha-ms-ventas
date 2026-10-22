package com.teraha.sales.controller;

import java.util.Optional;

import com.teraha.commons.dtos.ClientDTO;
import com.teraha.commons.dtos.SaleInvoiceDTO;
import com.teraha.sales.service.SaleInvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path="/sales", produces="application/json")
@CrossOrigin
@RequiredArgsConstructor
public class SaleInvoiceController {

    private final SaleInvoiceService saleInvoiceService;

    @GetMapping("/{id}")
    @Operation(summary = "Get Sale Invoice by ID", description = "Retrieves Sale Invoice with details by their unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sale Invoice found",
                    content = @Content(schema = @Schema(implementation = SaleInvoiceDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sale Invoice not found", content = @Content)
    })
    public ResponseEntity<SaleInvoiceDTO> findSaleInvoiceById(@PathVariable("id") Long id){
        SaleInvoiceDTO saleInvoice = saleInvoiceService.findSaleInvoiceById(id);
        return new ResponseEntity<>(saleInvoice, HttpStatus.OK);
    }

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new Sale Invoice", description = "Registers a new Sale Invoice in the system. Throws a conflict exception if the document already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sale Invoice created successfully",
                    content = @Content(schema = @Schema(implementation = SaleInvoiceDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input payload", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict: Identity Sale Invoice number already registered", content = @Content)
    })
    public SaleInvoiceDTO postSaleInvoice(@RequestBody SaleInvoiceDTO dto){
        return saleInvoiceService.createSaleInvoice(dto);
    }

    @GetMapping
    @Operation(summary = "List paginated Sale Invoices", description = "Returns a paginated list of Sale Invoices.")
    @ApiResponse(responseCode = "200", description = "Page of Sale Invoices retrieved successfully")
    public Page<SaleInvoiceDTO> getSaleInvoices(@RequestParam(name="page", defaultValue="0") int page, @RequestParam(name="search", required=false) String search){
        return saleInvoiceService.listSaleInvoices(page, search);
    }

}
