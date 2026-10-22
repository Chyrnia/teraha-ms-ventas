package com.teraha.sales.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.teraha.commons.entities.SaleInvoice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleInvoiceRepository extends InvoiceRepository<SaleInvoice> {


    @Query(
            value = """
                SELECT DISTINCT s
                FROM SaleInvoice s
                LEFT JOIN s.thirdParty c
                LEFT JOIN s.details d
                LEFT JOIN d.product p
                WHERE
                    LOWER(s.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.document) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM SaleInvoice s
                LEFT JOIN s.thirdParty c
                LEFT JOIN s.details d
                LEFT JOIN d.product p
                WHERE
                    LOWER(s.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.document) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                """
    )
    Page<SaleInvoice> searchSale(@Param("query") String query, Pageable pageable);
}
