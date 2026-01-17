package com.tool.sonarq.dto;

import lombok.Data;

@Data
public class Paging {
    private int pageIndex;
    private int pageSize;
    private int total;
}
