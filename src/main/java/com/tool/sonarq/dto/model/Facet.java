package com.tool.sonarq.dto.model;

import lombok.Data;

import java.util.List;

@Data
public class Facet {
    private String property;
    private List<FacetValue> values;
}

@Data
class FacetValue {
    private String val;
    private int count;
}
