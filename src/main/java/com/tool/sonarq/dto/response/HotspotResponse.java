package com.tool.sonarq.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tool.sonarq.dto.Hotspot;
import com.tool.sonarq.dto.Paging;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotspotResponse {
    private Paging paging;
    private List<Hotspot> hotspots;
}

