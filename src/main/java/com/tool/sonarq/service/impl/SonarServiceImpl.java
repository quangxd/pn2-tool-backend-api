package com.tool.sonarq.service.impl;

import com.tool.sonarq.client.SonarClient;
import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.dto.response.IssueExportData;
import com.tool.sonarq.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SonarServiceImpl implements SonarService {

    private final SonarClient sonarClient;

    @Async("sonar-tasks")
    @Override
    public CompletableFuture<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest) {
        return sonarClient
                .fetchAllIssues(repository, reportRequest)
                .toFuture();
    }
}
