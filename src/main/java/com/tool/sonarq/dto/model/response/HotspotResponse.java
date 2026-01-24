package com.tool.sonarq.dto.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.model.Hotspot;
import com.tool.sonarq.dto.model.Paging;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotspotResponse {
    private Paging paging;
    private List<Hotspot> hotspots;
}

