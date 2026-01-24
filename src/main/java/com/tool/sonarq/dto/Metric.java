package com.tool.sonarq.dto;

import lombok.Data;

@Data
public class Metric {
    private String key;
    private String name;
    private String description;
    private String domain;
    private String type;
    private boolean higherValuesAreBetter;
    private boolean qualitative;
    private boolean hidden;
    private Integer decimalScale;
    private String bestValue;
    private String worstValue;
}
