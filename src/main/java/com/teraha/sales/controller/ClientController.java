package com.teraha.sales.controller;

import java.util.Optional;

import com.teraha.commons.dtos.ClientDTO;
import com.teraha.sales.exception.ClientNotFoundException;
import com.teraha.sales.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/clients", produces="application/json")
@CrossOrigin
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Endpoints for managing and querying clients")
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves active client details by their unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client found",
                    content = @Content(schema = @Schema(implementation = ClientDTO.class))),
            @ApiResponse(responseCode = "404", description = "Client not found or inactive", content = @Content)
    })
    public ResponseEntity<ClientDTO> findClientById(
            @Parameter(description = "ID of the client to retrieve", required = true, example = "1")
            @PathVariable("id") Long id) {
        ClientDTO client = clientService.findClientById(id);
        return new ResponseEntity<>(client, HttpStatus.OK);
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create new client", description = "Registers a new client in the system. Throws a conflict exception if the document already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created successfully",
                    content = @Content(schema = @Schema(implementation = ClientDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input payload", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict: Identity document already registered", content = @Content)
    })
    public ResponseEntity<ClientDTO> postClient(
            @Valid @RequestBody ClientDTO dto) {
        ClientDTO created = clientService.createClient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List paginated clients", description = "Returns a paginated list of active clients.")
    @ApiResponse(responseCode = "200", description = "Page of clients retrieved successfully")
    public ResponseEntity<Page<ClientDTO>> getClients(
            @Parameter(description = "Page number to retrieve (starts at 0)", example = "0")
            @RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name = "search", required = false) String search){
        return ResponseEntity.ok(clientService.listClients(page, search));
    }

    @PutMapping(path="/{id}", consumes="application/json")
    @Operation(summary = "Update client", description = "Updates fields of an existing active client identified by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully",
                    content = @Content(schema = @Schema(implementation = ClientDTO.class))),
            @ApiResponse(responseCode = "404", description = "Client not found or inactive", content = @Content)
    })
    public ResponseEntity<ClientDTO> putClient(
            @Parameter(description = "ID of the client to update", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody ClientDTO dto) {
        Optional<ClientDTO> client = clientService.updateClient(id, dto);

        return client.map(ResponseEntity::ok).orElseThrow(() -> new ClientNotFoundException(id));

    }

    @DeleteMapping(path="/{id}")
    @Operation(summary = "Deactivate client", description = "Performs a soft delete by setting the client's status to inactive.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found or already inactive", content = @Content)
    })
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "ID of the client to deactivate", required = true, example = "1")
            @PathVariable("id") Long id) {
        boolean deleted = clientService.deactivateClient(id);

        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}