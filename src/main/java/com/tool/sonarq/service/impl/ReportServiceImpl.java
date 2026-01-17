package com.tool.sonarq.service.impl;

import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.dto.response.IssueExportData;
import com.tool.sonarq.service.ExcelService;
import com.tool.sonarq.service.ReportService;
import com.tool.sonarq.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SonarService sonarService;
    private final ExcelService excelService;

    @Override
    public byte[] generate(ReportRequest reportRequest) throws Exception {
        Map<String, CompletableFuture<IssueExportData>> futures =
                ofNullable(reportRequest.repositories())
                        .orElseGet(Set::of)
                        .stream()
                        .collect(toMap(
                                Function.identity(),
                                repo -> sonarService.fetchIssues(repo, reportRequest)
                        ));

        CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0])
        ).join();

        Map<String, IssueExportData> dataMap =
                futures.entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().join()
                ));

        return excelService.generateReport(dataMap, reportRequest.rowStartIndex());
    }
}
