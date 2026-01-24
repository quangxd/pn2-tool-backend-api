package com.tool.sonarq.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.ComponentMeasures;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeasureResponse {
    private ComponentMeasures component;
}

