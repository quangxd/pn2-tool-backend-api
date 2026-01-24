package com.tool.sonarq.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.Facet;
import com.tool.sonarq.dto.Issue;
import com.tool.sonarq.dto.Paging;
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
