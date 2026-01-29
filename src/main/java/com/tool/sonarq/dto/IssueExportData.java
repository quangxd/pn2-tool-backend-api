package com.tool.sonarq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueExportData {
    private String branch;
    private Integer securityHotspot;
    private Double hotspotReviewed;
    private Double coverage;
    private Double duplications;
    private Integer linesOfCode;
    private Integer linesToCover;
    private Integer coveredLines;
    private List<IssueDto> issues;
}
