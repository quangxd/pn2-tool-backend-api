package com.tool.sonarq.service;

import com.tool.sonarq.dto.request.ReportRequest;
import reactor.core.publisher.Mono;

public interface ReportService {
    Mono<byte[]> generate(ReportRequest reportRequest) throws Exception;
}
