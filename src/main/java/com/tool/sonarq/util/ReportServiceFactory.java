package com.tool.sonarq.util;

import com.tool.sonarq.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class ReportServiceFactory {

    private final Map<String, ReportService> services;

    public ReportServiceFactory(Map<String, ReportService> services) {
        this.services = services;
    }

    public ReportService getByType(String type) {
        log.info("Getting report service by type: {}", type);
        ReportService service = services.get(type + "ReportServiceImpl");
        log.info("Getting report service by type success with bean: {}", service);
        if (isNull(service)) {
            throw new IllegalArgumentException("Unsupported report type: " + type);
        }
        return service;
    }
}
