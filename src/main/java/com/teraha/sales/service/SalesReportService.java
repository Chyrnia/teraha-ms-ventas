
package com.teraha.sales.service;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import com.teraha.commons.dtos.ClientMonthlyReportDTO;
import com.teraha.commons.dtos.MonthlySalesSummaryDTO;
import com.teraha.sales.configs.PagingConfigs;
import com.teraha.sales.repository.SalesReportRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportService {

    private static final ZoneId REPORTING_ZONE =
            ZoneId.of("America/Asuncion");

    private final SalesReportRepository salesReportRepository;
    private final PagingConfigs pagingConfigs;

    public Page<ClientMonthlyReportDTO> getBestClientsBySales(
            int year,
            int month,
            int page) {

        YearMonth period = validatePeriod(year, month);
        Pageable pageable = createPageable(page);

        return salesReportRepository.findBestClientsBySales(
                period.atDay(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime(),
                period.plusMonths(1).atDay(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime(),
                pageable
        );
    }

    public Page<ClientMonthlyReportDTO> getBestClientsByRevenue(
            int year,
            int month,
            int page) {

        YearMonth period = validatePeriod(year, month);
        Pageable pageable = createPageable(page);

        return salesReportRepository.findBestClientsByRevenue(
                period.atDay(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime(),
                period.plusMonths(1).atDay(1).atStartOfDay(REPORTING_ZONE).toOffsetDateTime(),
                pageable
        );
    }

    public MonthlySalesSummaryDTO getMonthlySummary(
            int year,
            int month) {

        YearMonth period = validatePeriod(year, month);

        OffsetDateTime start = period.atDay(1)
                .atStartOfDay(REPORTING_ZONE)
                .toOffsetDateTime();

        OffsetDateTime end = period.plusMonths(1)
                .atDay(1)
                .atStartOfDay(REPORTING_ZONE)
                .toOffsetDateTime();

        return salesReportRepository.getMonthlySummary(start, end);
    }

    private YearMonth validatePeriod(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reporting period: " + year + "-" + month
            );
        }
    }

    private Pageable createPageable(int page) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        return PageRequest.of(page, pagingConfigs.getPageSize());
    }
}

