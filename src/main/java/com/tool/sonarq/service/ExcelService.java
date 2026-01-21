package com.tool.sonarq.service;

import com.tool.sonarq.dto.response.IssueExportData;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ExcelService {
    Mono<byte[]> generateReport(Map<String, IssueExportData> dataMap, Integer rowStartIndex);
}
