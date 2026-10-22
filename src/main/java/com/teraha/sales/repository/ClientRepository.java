package com.teraha.sales.repository;

import java.util.Optional;

import com.teraha.commons.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.teraha.commons.entities.Client;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends BaseRepository<Client> {

    Optional<Client> findByIdAndActiveTrue(Long id);

    Page<Client> findAllByActiveTrue(Pageable page);

    boolean existsByDocument(String document);

    @Query("""
		SELECT c
		FROM Client c
		WHERE c.active = true
		  AND (
			LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
			OR LOWER(c.document) LIKE LOWER(CONCAT('%', :query, '%'))
			OR LOWER(c.timbrado) LIKE LOWER(CONCAT('%', :query, '%'))
		  )
		  """)
    Page<Client> searchActiveClients(@Param("query") String query,
                                         Pageable pageable);
}
