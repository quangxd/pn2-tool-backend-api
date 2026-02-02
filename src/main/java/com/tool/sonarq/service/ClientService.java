package com.tool.sonarq.service;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.request.ReportRequest;
import reactor.core.publisher.Mono;

public interface ClientService {
    Mono<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest);
}
