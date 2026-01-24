package com.tool.sonarq.dto.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Branch {
    private String name;
    private boolean isMain;
    private String type;
    private Status status;
    private String analysisDate;
    private boolean excludedFromPurge;
    private String branchId;
}
