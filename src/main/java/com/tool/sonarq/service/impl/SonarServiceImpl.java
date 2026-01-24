package com.tool.sonarq.service.impl;

import com.tool.sonarq.client.SonarClient;
import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SonarServiceImpl implements SonarService {

    private final SonarClient sonarClient;

    @Override
    public Mono<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest) {
        return sonarClient.fetchAllIssues(repository, reportRequest);
    }
}
