package com.tool.sonarq.service;

import com.tool.sonarq.dto.request.ReportRequest;

public interface ReportService {
    byte[] generate(ReportRequest reportRequest) throws Exception;
}
