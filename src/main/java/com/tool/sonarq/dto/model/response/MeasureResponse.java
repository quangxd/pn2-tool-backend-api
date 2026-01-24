package com.tool.sonarq.dto.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.model.ComponentMeasures;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeasureResponse {
    private ComponentMeasures component;
}

