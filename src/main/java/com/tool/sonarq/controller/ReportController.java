package com.tool.sonarq.controller;

import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.service.impl.ReportServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceImpl reportService;

    @GetMapping("/sonar/health")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                )
        );
    }

    @PostMapping("/sonar/export")
    public Mono<ResponseEntity<byte[]>> generateReport(@RequestBody ReportRequest request) {
        log.info("[ReportController] Generating report for {}", request.repositories());
        return reportService.generate(request)
                .map(this::toReportResponse);
    }

    private ResponseEntity<byte[]> toReportResponse(byte[] file) {
        String filename = format("sonarq_report_%s.xlsx", now().format(ofPattern("dd_MM_yyyy_HH_mm_ss")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}
