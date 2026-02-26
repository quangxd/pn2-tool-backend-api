package com.tool.sonarq.service.abstracts;

import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.exception.BizException;
import com.tool.sonarq.service.ClientService;
import com.tool.sonarq.service.ExcelService;
import com.tool.sonarq.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public abstract class AbstractReportService implements ReportService {

    protected static final int CONCURRENCY = 5;
    protected final ClientService clientService;
    protected final ExcelService excelService;

    @Override
    public Mono<byte[]> generate(ReportRequest reportRequest) {
        return processGenerate(reportRequest)
                .flatMap(data -> excelService.generateReport(data, reportRequest.rowStartIndex()))
                .onErrorMap(e -> new BizException(e.getMessage()));
    }

    protected abstract Mono<Map<String, IssueExportData>> processGenerate(ReportRequest reportRequest);

    protected boolean isEmptyIssues(IssueExportData issueExportData) {
        return issueExportData.getIssues().isEmpty();
    }
}
