package com.tool.sonarq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentMeasures {
    private String key;
    private String name;
    private List<Measure> measures;
    private List<Metric> metrics;
}
