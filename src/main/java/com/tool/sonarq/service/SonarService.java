package com.tool.sonarq.service;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.request.ReportRequest;
import reactor.core.publisher.Mono;

public interface SonarService {
    Mono<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest);
}
