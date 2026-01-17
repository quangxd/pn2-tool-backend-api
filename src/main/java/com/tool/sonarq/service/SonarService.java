package com.tool.sonarq.service;

import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.dto.response.IssueExportData;

import java.util.concurrent.CompletableFuture;

public interface SonarService {
    CompletableFuture<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest);
}
