package com.tool.sonarq.service;

import com.tool.sonarq.dto.response.IssueExportData;

import java.io.IOException;
import java.util.Map;

public interface ExcelService {
    byte[] generateReport(Map<String, IssueExportData> dataMap, Integer rowStartIndex) throws IOException;
}
