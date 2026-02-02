package com.tool.sonarq.service;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.request.ReportRequest;
import reactor.core.publisher.Mono;

public interface ClientService {
    String SONA_CLIENT_SERVICE_IMPL = "SonarClientServiceImpl";
    Mono<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest);
}
