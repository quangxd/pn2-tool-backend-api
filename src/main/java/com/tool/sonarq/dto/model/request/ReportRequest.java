package com.tool.sonarq.dto.model.request;

import java.util.Set;

public record ReportRequest(
        String cookie,
        int pageSize,
        int pageNumber,
        Set<String> repositories,
        Set<String> statuses,
        Integer rowStartIndex
) {}
