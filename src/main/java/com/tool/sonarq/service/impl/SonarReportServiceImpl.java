package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.service.AbstractReportService;
import com.tool.sonarq.service.ClientService;
import com.tool.sonarq.service.ExcelService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static reactor.core.publisher.Flux.fromIterable;

@Service("sonarqReportServiceImpl")
public class SonarReportServiceImpl extends AbstractReportService {

    public SonarReportServiceImpl(@Qualifier("sonarServiceImpl") ClientService clientService, ExcelService excelService) {
        super(clientService, excelService);
    }

    @Override
    protected Mono<Map<String, IssueExportData>> innerHandler(ReportRequest reportRequest) {
        return fromIterable(reportRequest.repositories())
                .flatMap(repo -> toIssueExportDataEntry(repo, reportRequest, clientService), CONCURRENCY)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    protected Mono<Map.Entry<String, IssueExportData>> toIssueExportDataEntry(String repositoryName,
                                                                              ReportRequest reportRequest,
                                                                              ClientService clientService) {
        return clientService.fetchIssues(repositoryName, reportRequest)
                .filter(issueExportData -> !isEmptyIssues(issueExportData))
                .map(issues -> Map.entry(repositoryName, issues));
    }

    protected boolean isEmptyIssues(IssueExportData issueExportData) {
        return issueExportData.getIssues().isEmpty();
    }
}
