
package com.teraha.sales.controller;

import com.teraha.commons.dtos.ClientMonthlyReportDTO;
import com.teraha.commons.dtos.MonthlySalesSummaryDTO;
import com.teraha.sales.service.SalesReportService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/reports/sales", produces = "application/json")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SalesReportController {

    private final SalesReportService salesReportService;

    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySalesSummaryDTO> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                salesReportService.getMonthlySummary(year, month)
        );
    }

    @GetMapping("/clients/best-sellers")
    public ResponseEntity<Page<ClientMonthlyReportDTO>> getBestClientsBySales(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                salesReportService.getBestClientsBySales(year, month, page)
        );
    }

    @GetMapping("/clients/highest-revenue")
    public ResponseEntity<Page<ClientMonthlyReportDTO>> getBestClientsByRevenue(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                salesReportService.getBestClientsByRevenue(year, month, page)
        );
    }
}

