package com.tool.sonarq.controller;

import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.util.ReportServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    private final ReportServiceFactory reportServiceFactory;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                )
        );
    }

    @PostMapping("/report/export")
    public Mono<ResponseEntity<byte[]>> export(@RequestParam(defaultValue = "Sonar", required = false) String type,
                                               @RequestBody ReportRequest request) {
        log.info("[ReportController] Generating {} report for {}", type, request.repositories());
        long startTime = System.currentTimeMillis();

        return reportServiceFactory.getByType(type).generate(request)
                .elapsed()
                .doOnNext(tuple -> {
                    long duration = tuple.getT1();
                    log.info("[ReportController] Service generation took {} ms ({} seconds)", duration, duration / 1000.0);
                })
                .map(tuple -> this.toReportResponse(type, tuple.getT2()))
                .doOnSuccess(response -> {
                    long totalDuration = System.currentTimeMillis() - startTime;
                    log.info("[ReportController] Total controller time took {} ms ({} seconds) for {} repositories",
                            totalDuration, totalDuration / 1000.0, request.repositories().size());
                });
    }

    private ResponseEntity<byte[]> toReportResponse(String type, byte[] file) {
        String filename = format("%s_report_%s.xlsx", type, now().format(ofPattern("dd_MM_yyyy_HH_mm_ss")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}
