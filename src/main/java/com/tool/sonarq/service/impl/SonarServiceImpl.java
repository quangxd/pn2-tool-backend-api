package com.tool.sonarq.service.impl;

import com.tool.sonarq.client.SonarClient;
import com.tool.sonarq.dto.IssueDto;
import com.tool.sonarq.dto.IssueExportData;
import com.tool.sonarq.dto.model.*;
import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.dto.model.response.BranchResponse;
import com.tool.sonarq.dto.model.response.HotspotResponse;
import com.tool.sonarq.dto.model.response.MeasureResponse;
import com.tool.sonarq.dto.model.response.SearchResponse;
import com.tool.sonarq.service.SonarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.lang.Math.subtractExact;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static reactor.core.publisher.Mono.zip;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarServiceImpl implements SonarService {

    private final SonarClient sonarClient;

    @Override
    public Mono<IssueExportData> fetchIssues(String repository, ReportRequest reportRequest) {
        return assembleExportData(repository, reportRequest);
    }

    private Mono<IssueExportData> assembleExportData(String repository, ReportRequest reportRequest) {
        return zip(
                sonarClient.queryIssues(repository, reportRequest),
                sonarClient.queryBranch(repository, reportRequest.cookie()),
                sonarClient.queryMeasures(repository, reportRequest.cookie()),
                sonarClient.queryHotspots(repository, reportRequest.cookie())
        ).map(tuples ->
                toExportData(tuples.getT1(), tuples.getT2(), tuples.getT3(), tuples.getT4()));
    }

    private IssueExportData toExportData(SearchResponse searchResponse, BranchResponse branchResponse,
                                               MeasureResponse measureResponse, HotspotResponse hotspotResponse) {
        List<IssueDto> issueDtos = ofNullable(searchResponse.getIssues())
                .map(issues -> toListIssuesDto(issues, hotspotResponse))
                .orElseGet(List::of);

        String branchName = ofNullable(branchResponse.getBranches())
                .flatMap(branches -> branches.stream().findFirst())
                .map(Branch::getName)
                .orElse(EMPTY);

        Double duplications = ofNullable(measureResponse.getComponent())
                .map(ComponentMeasures::getMeasures)
                .stream()
                .flatMap(List::stream)
                .filter(measure -> "duplicated_lines_density".equals(measure.getMetric()))
                .findFirst()
                .map(Measure::getValue)
                .map(Double::parseDouble)
                .map(duplicationsPercentage -> duplicationsPercentage / 100.0)
                .orElse(0.0);

        Integer linesOfCode = ofNullable(measureResponse.getComponent())
                .map(ComponentMeasures::getMeasures)
                .stream()
                .flatMap(List::stream)
                .filter(measure -> "ncloc".equals(measure.getMetric()))
                .findFirst()
                .map(Measure::getValue)
                .map(Integer::parseInt)
                .orElse(0);

        Integer linesToCover = ofNullable(measureResponse.getComponent())
                .map(ComponentMeasures::getMeasures)
                .stream()
                .flatMap(List::stream)
                .filter(measure -> "lines_to_cover".equals(measure.getMetric()))
                .findFirst()
                .map(Measure::getValue)
                .map(Integer::parseInt)
                .orElse(0);

        Integer uncoveredLines = ofNullable(measureResponse.getComponent())
                .map(ComponentMeasures::getMeasures)
                .stream()
                .flatMap(List::stream)
                .filter(measure -> "uncovered_lines".equals(measure.getMetric()))
                .findFirst()
                .map(Measure::getValue)
                .map(Integer::parseInt)
                .orElse(0);

        Integer coveredLine = subtractExact(linesOfCode, uncoveredLines);

        return IssueExportData.builder()
                .issues(issueDtos)
                .branch(branchName)
                .duplications(duplications)
                .linesOfCode(linesOfCode)
                .linesToCover(linesToCover)
                .coveredLines(coveredLine)
                .build();
    }

    private List<IssueDto> toListIssuesDto(List<Issue> issues, HotspotResponse hotspotResponse) {
        return issues.stream()
                .map(issue -> toIssueDto(issue, hotspotResponse))
                .toList();
    }

    private IssueDto toIssueDto(Issue issue, HotspotResponse hotspotResponse) {
        IssueDto dto = new IssueDto();
        BeanUtils.copyProperties(issue, dto);
        String hotspot = ofNullable(hotspotResponse)
                .map(HotspotResponse::getHotspots)
                .stream()
                .flatMap(List::stream)
                .filter(hs -> isHotspotBelongToIssue(issue, hs))
                .findFirst()
                .map(Hotspot::getStatus)
                .orElse(EMPTY);
        dto.setHotspot(hotspot);

        return dto;
    }

    private boolean isHotspotBelongToIssue(Issue issue, Hotspot hotspot) {
        return hotspot.getComponent().equals(issue.getComponent())
                && hotspot.getLine().equals(issue.getLine());
    }
}
