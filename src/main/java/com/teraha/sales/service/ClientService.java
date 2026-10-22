package com.teraha.sales.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teraha.commons.dtos.ClientDTO;
import com.teraha.commons.entities.Client;
import com.teraha.sales.configs.PagingConfigs;
import com.teraha.sales.exception.ClientConflictException;
import com.teraha.sales.exception.ClientNotFoundException;
import com.teraha.sales.mapper.ClientMapper;
import com.teraha.sales.repository.ClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ClientService {

    private final PagingConfigs pageConfigs;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientDTO createClient(ClientDTO dto) {
        if (dto.getDocument() != null && clientRepository.existsByDocument(dto.getDocument())) {
            log.warn("Attempted to create client with duplicate document: {}", dto.getDocument());
            throw new ClientConflictException(dto.getDocument());
        }

        log.info("Creating client with document: {}", dto.getDocument());

        Client client = clientMapper.toEntity(dto);
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public ClientDTO findClientById(Long id) {
        return clientRepository.findByIdAndActiveTrue(id)
                .map(clientMapper::toDto)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    public Optional<ClientDTO> updateClient(Long id, ClientDTO dto) {
        // Solo permite actualizar clientes que estén ACTIVOS
        return clientRepository.findByIdAndActiveTrue(id)
                .map(client -> {
                    log.info("Updating client with ID: {}", id);
                    clientMapper.updateEntity(dto, client);
                    // Debido al estado gestionado por JPA (@Transactional),
                    // el save actualiza la entidad en BD automáticamente al terminar el método
                    return clientMapper.toDto(clientRepository.save(client));
                });
    }

    public boolean deactivateClient(Long id) {
        // Solo busca e intenta desactivar clientes que actualmente están ACTIVOS
        Optional<Client> optClient = clientRepository.findByIdAndActiveTrue(id);
        if (optClient.isPresent()) {
            log.info("Deactivating client with ID: {}", id);
            Client client = optClient.get();
            client.setActive(false);
            clientRepository.save(client);
            return true;
        }
        log.warn("Client with ID {} not found or already deactivated", id);
        return false;
    }

    @Transactional(readOnly = true)
    public Page<ClientDTO> listClients(int page, String search) {
        Pageable pageReq = PageRequest.of(page, pageConfigs.getPageSize());
        Page<Client> clients;
        if (search == null || search.isBlank()) {
            log.info("Listing clients, page {}", page);
            clients = clientRepository.findAllByActiveTrue(pageReq);
        } else {
            log.info("Listing clients, page {}, search '{}", page, search.trim());
            clients = clientRepository.searchActiveClients(search.trim(), pageReq);
        }
        return clients.map(clientMapper::toDto);
    }
}