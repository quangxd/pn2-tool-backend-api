package com.tool.sonarq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Measure {
    private String metric;
    private String value;
    private boolean bestValue;
}
