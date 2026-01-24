package com.tool.sonarq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hotspot {
    private String key;
    private String component;
    private String securityCategory;
    private String vulnerabilityProbability;
    private String status;
    private Integer line;
    private String message;
    private String author;
    private String ruleKey;
}
