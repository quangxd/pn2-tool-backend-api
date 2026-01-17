package com.tool.sonarq.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.Branch;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchResponse {
    private List<Branch> branches;
}
