
package com.teraha.sales.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.teraha.commons.dtos.ClientMonthlyReportDTO;
import com.teraha.commons.dtos.MonthlySalesSummaryDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SalesReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String CLIENTS_QUERY = """
        SELECT
            c.id AS client_id,
            c.name AS name,
            c.document AS document,
            COUNT(si.id) AS total_sales,
            SUM(si.total) AS total_amount
        FROM sale_invoices si
        JOIN clients c ON c.id = si.client_id
        WHERE si.transaction_date >= :start
          AND si.transaction_date < :end
        GROUP BY c.id, c.name, c.document
        """;

    private static final String COUNT_CLIENTS_QUERY = """
        SELECT COUNT(*)
        FROM (
            SELECT si.client_id
            FROM sale_invoices si
            WHERE si.transaction_date >= :start
              AND si.transaction_date < :end
            GROUP BY si.client_id
        ) AS monthly_clients
        """;

    private static final String SUMMARY_QUERY = """
        SELECT
            COALESCE(SUM(si.total), 0) AS total_revenue,
            COUNT(si.id) AS total_sales,
            COALESCE(AVG(si.total), 0) AS average_ticket,
            COUNT(DISTINCT si.client_id) AS clients_attended
        FROM sale_invoices si
        WHERE si.transaction_date >= :start
          AND si.transaction_date < :end
        """;

    public Page<ClientMonthlyReportDTO> findBestClientsBySales(
            OffsetDateTime start,
            OffsetDateTime end,
            Pageable pageable) {

        return findClientRanking(
                start,
                end,
                pageable,
                "total_sales DESC, c.id ASC"
        );
    }

    public Page<ClientMonthlyReportDTO> findBestClientsByRevenue(
            OffsetDateTime start,
            OffsetDateTime end,
            Pageable pageable) {

        return findClientRanking(
                start,
                end,
                pageable,
                "total_amount DESC, c.id ASC"
        );
    }

    private Page<ClientMonthlyReportDTO> findClientRanking(
            OffsetDateTime start,
            OffsetDateTime end,
            Pageable pageable,
            String orderBy) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        String query = CLIENTS_QUERY + """
            ORDER BY %s
            LIMIT :limit OFFSET :offset
            """.formatted(orderBy);

        List<ClientMonthlyReportDTO> clients = jdbc.query(
                query,
                params,
                (rs, rowNum) -> {
                    ClientMonthlyReportDTO dto = new ClientMonthlyReportDTO();

                    dto.setClientId(rs.getLong("client_id"));
                    dto.setName(rs.getString("name"));
                    dto.setDocument(rs.getString("document"));
                    dto.setTotalSales(rs.getLong("total_sales"));
                    dto.setTotalAmount(rs.getBigDecimal("total_amount"));

                    return dto;
                }
        );

        Long totalElements = jdbc.queryForObject(
                COUNT_CLIENTS_QUERY,
                params,
                Long.class
        );

        return new PageImpl<>(
                clients,
                pageable,
                totalElements != null ? totalElements : 0L
        );
    }

    public MonthlySalesSummaryDTO getMonthlySummary(
            OffsetDateTime start,
            OffsetDateTime end) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end);

        return jdbc.queryForObject(
                SUMMARY_QUERY,
                params,
                (rs, rowNum) -> {
                    MonthlySalesSummaryDTO dto = new MonthlySalesSummaryDTO();

                    dto.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                    dto.setTotalSales(rs.getLong("total_sales"));
                    dto.setAverageTicket(rs.getBigDecimal("average_ticket"));
                    dto.setClientsAttended(rs.getLong("clients_attended"));

                    return dto;
                }
        );
    }
}

