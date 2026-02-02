package com.tool.sonarq.service;

import com.tool.sonarq.dto.model.request.ReportRequest;
import reactor.core.publisher.Mono;

public interface ReportService {
    String SONAR_REPORT_SERVICE_IMPL = "SonarReportServiceImpl";
    Mono<byte[]> generate(ReportRequest reportRequest);
}
