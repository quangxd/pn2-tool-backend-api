package com.tool.sonarq.dto.response;

import com.tool.sonarq.dto.Issue;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IssueExportData {
    private String branch;
    private List<Issue> issues;
}
