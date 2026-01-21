package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.service.ExcelService;
import com.tool.sonarq.service.ReportService;
import com.tool.sonarq.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static java.util.Map.entry;
import static reactor.core.publisher.Flux.fromIterable;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SonarService sonarService;
    private final ExcelService excelService;

    @Override
    public Mono<byte[]> generate(ReportRequest reportRequest) {
        return fromIterable(reportRequest.repositories())
                .flatMap(
                        repo ->
                                sonarService.fetchIssues(repo, reportRequest)
                                        .map(issues -> entry(repo, issues)),
                        5
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(data -> excelService.generateReport(data, reportRequest.rowStartIndex()));
    }
}
