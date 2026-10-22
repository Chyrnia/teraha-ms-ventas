package com.teraha.sales.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teraha.commons.entities.Product;

public interface ProductRepository extends BaseRepository<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    Page<Product> findAllByActiveTrue(Pageable page);

    @Query("""
       SELECT p FROM Product p
       WHERE p.active = true
       AND (
          LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
       )
       """)
    Page<Product> searchActiveProducts(@Param("query") String query, Pageable page);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
