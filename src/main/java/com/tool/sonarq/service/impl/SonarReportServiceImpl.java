package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.service.ClientService;
import com.tool.sonarq.service.ExcelService;
import com.tool.sonarq.service.abstracts.AbstractReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.tool.sonarq.service.ClientService.SONA_CLIENT_SERVICE_IMPL;
import static com.tool.sonarq.service.ReportService.SONAR_REPORT_SERVICE_IMPL;
import static reactor.core.publisher.Flux.fromIterable;

@Service(SONAR_REPORT_SERVICE_IMPL)
public class SonarReportServiceImpl extends AbstractReportService {

    public SonarReportServiceImpl(@Qualifier(SONA_CLIENT_SERVICE_IMPL) ClientService clientService, ExcelService excelService) {
        super(clientService, excelService);
    }

    @Override
    protected Mono<Map<String, IssueExportData>> processGenerate(ReportRequest reportRequest) {
        return fromIterable(reportRequest.repositories())
                .flatMap(repositoryName ->
                        toIssueExportDataEntryMono(repositoryName, reportRequest, clientService), CONCURRENCY)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Mono<Map.Entry<String, IssueExportData>> toIssueExportDataEntryMono(String repositoryName,
                                                                              ReportRequest reportRequest,
                                                                              ClientService clientService) {
        return clientService.fetchIssues(repositoryName, reportRequest)
                .filter(issueExportData -> !isEmptyIssues(issueExportData))
                .map(issues -> Map.entry(repositoryName, issues));
    }
}
