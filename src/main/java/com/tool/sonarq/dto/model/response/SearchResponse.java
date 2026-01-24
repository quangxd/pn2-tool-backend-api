package com.tool.sonarq.dto.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.model.Facet;
import com.tool.sonarq.dto.model.Issue;
import com.tool.sonarq.dto.model.Paging;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {
    private int total;
    private int p;
    private int ps;
    private Paging paging;
    private int effortTotal;
    private List<Issue> issues;
    private List<Facet> facets;
}
