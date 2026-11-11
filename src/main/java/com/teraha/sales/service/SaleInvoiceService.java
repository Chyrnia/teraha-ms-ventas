package com.teraha.sales.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigDecimal;

import com.teraha.commons.dtos.InventoryUpdate;
import com.teraha.commons.dtos.InventoryUpdateLine;
import com.teraha.commons.entities.Product;
import com.teraha.sales.exception.ProductNotFoundException;
import com.teraha.sales.exception.SaleInvoiceBadRequestException;

import jakarta.persistence.EntityManager;

import com.teraha.commons.dtos.SaleInvoiceDetailDTO;
import com.teraha.sales.exception.ClientNotFoundException;
import com.teraha.sales.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.teraha.commons.entities.SaleInvoice;
import com.teraha.commons.dtos.SaleInvoiceDTO;
import com.teraha.commons.entities.SaleInvoiceDetail;
import com.teraha.commons.entities.Client;

import com.teraha.sales.repository.SaleInvoiceRepository;
import com.teraha.sales.repository.ProductRepository;
import com.teraha.sales.mapper.SaleInvoiceMapper;
import com.teraha.sales.configs.PagingConfigs;
import com.teraha.sales.exception.SaleInvoiceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class SaleInvoiceService {

    @Value("${products.service.url}")
    private String productServiceUrl;
    private final PagingConfigs pageConfigs;
    private final SaleInvoiceRepository saleInvoiceRepository;
    private final SaleInvoiceMapper saleInvoiceMapper;
    private final RestTemplate rest;
    private final ClientRepository clientRepository;
    private final EntityManager entityManager;
    private final ProductRepository productRepository;

    @CachePut(cacheNames = "sales:invoices", key = "#result.id")
    public SaleInvoiceDTO createSaleInvoice(SaleInvoiceDTO dto) {
        log.info("Starting creation of sale invoice for client ID {}", dto != null ? dto.getClientId() : null);

        validateCreationDto(dto);

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(dto.getClientId()));

        SaleInvoice saleInvoice = new SaleInvoice(dto.getTransactionDate(), client);

        // 1. Llenar detalles y calcular total
        BigDecimal total = fillInvoiceWithDetails(saleInvoice, dto.getDetails());
        saleInvoice.setTotal(total);

        // 2. Actualizar stock en el microservicio de productos

		updateProductStocks(saleInvoice.getDetails(), -1);

        try {

            SaleInvoice saved = saleInvoiceRepository.saveAndFlush(saleInvoice);
            entityManager.refresh(saved);
            log.info("Sale invoice successfully saved with ID {}", saved.getId());
            return saleInvoiceMapper.toDto(saved);

        } catch (Exception e) {
            log.error("Failed to save invoice locally. Compensating inventory stock...", e);

			// Si falla algo en sales, se revierte el descuento en products
			try {
				// revertir el descuento de stock
				reverseProductStocks(saleInvoice.getDetails());
			} catch (Exception ex) {
				log.error("CRITICAL: Failed to compensate stock for products", ex);
			}

            throw new SaleInvoiceBadRequestException("Error saving sale invoice. Stock has been reverted.");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "sales:invoices", key = "#id")
    public SaleInvoiceDTO findSaleInvoiceById(Long id) {
        log.info("Fetching sale invoice with ID: {}", id);
        return saleInvoiceRepository.findById(id)
                .map(saleInvoiceMapper::toDto)
                .orElseThrow(() -> {
                        log.error("Sale invoice not found with ID: {}", id);
                        return new SaleInvoiceNotFoundException(id);
                });
    }

    @Transactional(readOnly = true)
    public Page<SaleInvoiceDTO> listSaleInvoices(int page, String search) {
        Pageable pageReq = PageRequest.of(page, pageConfigs.getPageSize());
        Page<SaleInvoice> invoices;
        if (search == null || search.isBlank()) {
            log.info("Listing sales, page {}", page);
            invoices = saleInvoiceRepository.findAll(pageReq);
        } else {
            log.info("Listing sales, page {}, search term: '{}'", page, search.trim());
            invoices = saleInvoiceRepository.searchSale(search.trim(), pageReq);
        }
        return invoices.map(saleInvoiceMapper::toDto);
    }

	@CacheEvict(cacheNames = "sales:invoices", key = "#id")
	public void deactivateSaleInvoice(Long id) {

		SaleInvoice invoice = saleInvoiceRepository
			.findByIdForUpdate(id)
			.orElseThrow(() -> new SaleInvoiceNotFoundException(id));

		if (invoice.isCancelled()) {
			throw new SaleInvoiceBadRequestException(
					"Sale invoice is already cancelled"
					);
		}

		log.info("Cancelling sale invoice {}", id);

		// Restore the stock deducted when this sale was registered.
		reverseProductStocks(invoice.getDetails());

		// Only cancel after Products accepts the stock restoration.
		invoice.setCancelled(true);
	}

	private void validateCreationDto(SaleInvoiceDTO dto){
		if (dto.getTransactionDate() == null) {
			log.error("No invoice date provided");
			throw new SaleInvoiceBadRequestException("Date invalid");
		}

		if (dto.getClientId() == null) {
			log.error("No client provided");
			throw new SaleInvoiceBadRequestException("Client invalid");
		}

		if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
			log.error("No details provided");
			throw new SaleInvoiceBadRequestException("Empty details");
		}
	}

	private BigDecimal fillInvoiceWithDetails(SaleInvoice sale, List<SaleInvoiceDetailDTO> details){
		//Validacion de details
		BigDecimal total = BigDecimal.ZERO;
		log.debug("Processing {} detail items for sale invoice", details.size());

		for (SaleInvoiceDetailDTO d : details) {

			// validate productId
			if (d.getProductId() == null) {
				log.error("Validation failed in detail: Product ID is null");
				throw new SaleInvoiceBadRequestException("Required productId");
			}

			// retrieve and validate product
			Product product = productRepository
				.findByIdAndActiveTrue(d.getProductId())
				.orElseThrow(() -> {
					log.error("Product not found or inactive with ID: {}", d.getProductId());
					return new ProductNotFoundException(d.getProductId());
				});


			// validate quantity
			if (d.getQuantity() == null || d.getQuantity() <= 0) {
				log.error("Validation failed in detail: Quantity ({}) must be > 0 for product ID {}", d.getQuantity(), d.getProductId());
				throw new SaleInvoiceBadRequestException("Required quantity > 0");
			}
			BigDecimal quantity = BigDecimal.valueOf(d.getQuantity());

			BigDecimal unitPrice = product.getPrice();

			// validate unit price
			if (d.getUnitPrice() == null || d.getUnitPrice() <= 0) {
				log.error("Validation failed in detail: Unit price ({}) must be > 0 for product ID {}", d.getUnitPrice(), d.getProductId());
				throw new SaleInvoiceBadRequestException("Required unit price > 0");
			}

			SaleInvoiceDetail detail = new SaleInvoiceDetail(product, d.getQuantity(), unitPrice);
			sale.addDetail(detail);

			BigDecimal subtotal = unitPrice.multiply(quantity);
			total = total.add(subtotal);

			log.debug("Added detail - Product ID: {}, Quantity: {}, Unit Price: {}, Subtotal: {}",
					d.getProductId(), d.getQuantity(), unitPrice, subtotal);
		}

		return total;
	}

	private BigDecimal computeTotal(SaleInvoice sale){
		return BigDecimal.ONE;
	}

	private void updateProductStocks(
			List<SaleInvoiceDetail> details,
			int multiplier
			) 
	{
		applyProductStockChanges(details, multiplier);
	}

	private void reverseProductStocks(List<SaleInvoiceDetail> details) {
		applyProductStockChanges(details, 1);
	}

	private void applyProductStockChanges(
			List<SaleInvoiceDetail> details,
			int multiplier
			) 
	{
		// Aggregate quantities by product ID.
		Map<Long, Integer> movements = new LinkedHashMap<>();

		for (SaleInvoiceDetail detail : details) {

			Long productId = detail.getProduct().getId();
			int delta = Math.multiplyExact(detail.getQuantity(), multiplier);

			movements.merge(
					productId,
					delta,
					Math::addExact
					);
		}

		List<InventoryUpdateLine> lines = new ArrayList<>();

		for (Map.Entry<Long, Integer> movement : movements.entrySet()) {

			InventoryUpdateLine line = new InventoryUpdateLine();

			line.setProductId(movement.getKey());
			line.setStockDelta(movement.getValue());

			// No unitCost: sales never modify product cost.

			lines.add(line);
		}

		InventoryUpdate update = new InventoryUpdate();
		update.setDetails(lines);

		try {
			rest.postForObject(
					productServiceUrl + "/products/inventory",
					update,
					Void.class
					);

			log.info(
					"Applied sales inventory batch with {} product movements",
					lines.size()
					);

		} catch (Exception e) {
			log.error("Failed to apply sales inventory batch", e);
			throw new SaleInvoiceBadRequestException(
					"Error updating product inventory"
					);
		}
			}
}
