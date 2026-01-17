package com.tool.sonarq.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.service.impl.ReportServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tools")
public class ReportController {

    private final ReportServiceImpl reportService;
    public ReportController(ReportServiceImpl reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                )
        );
    }

    @PostMapping("/sonar/export")
    public ResponseEntity<byte[]> generateReport(
            @RequestBody ReportRequest request) throws Exception {

        byte[] file = reportService.generate(request);

        String filename = "sonarq_report_" +
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss"))
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}
