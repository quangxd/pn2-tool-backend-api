package com.tool.sonarq.dto;

import lombok.Data;
import java.util.List;

@Data
public class IssueDto {
    private String key;
    private String type;
    private String status;
    private String rule;
    private String component;
    private Integer line;
    private String message;
    private String effort;
    private String author;
    private String hotspot;
    private String creationDate;
    private String updateDate;
    private List<Impact> impacts;
    private List<String> tags;
}
