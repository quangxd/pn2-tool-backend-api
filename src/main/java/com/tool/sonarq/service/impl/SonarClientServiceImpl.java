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
import com.tool.sonarq.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.tool.sonarq.service.ClientService.SONA_CLIENT_SERVICE_IMPL;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static reactor.core.publisher.Mono.zip;

@Slf4j
@Service(SONA_CLIENT_SERVICE_IMPL)
@RequiredArgsConstructor
public class SonarClientServiceImpl implements ClientService {

    private static final String REVIEWED = "REVIEWED";
    private static final String ACKNOWLEDGED = "ACKNOWLEDGED";

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
                sonarClient.queryHotspots(repository, reportRequest.cookie()),
                sonarClient.queryHotspotsWithStatusAndResolution(repository, reportRequest.cookie(), REVIEWED, ACKNOWLEDGED)
        ).map(tuples ->
                toExportData(tuples.getT1(), tuples.getT2(), tuples.getT3(), tuples.getT4(), tuples.getT5()));
    }

    private IssueExportData toExportData(SearchResponse searchResponse, BranchResponse branchResponse,
                                               MeasureResponse measureResponse, HotspotResponse toReviewHotspotResponse,
                                         HotspotResponse acknowledgedHotspotResponse) {
        List<IssueDto> issueDtos = ofNullable(searchResponse.getIssues())
                .map(issues -> toListIssuesDto(issues, toReviewHotspotResponse))
                .orElseGet(List::of);

        String branchName = ofNullable(branchResponse.getBranches())
                .flatMap(branches -> branches.stream().findFirst())
                .map(Branch::getName)
                .orElse(EMPTY);

        //Tổng số hotspots: Chuyển sang lấy từ Tab Hotspots =  (To review) + Acknowledge
        int securityHotspot = addExact(toReviewHotspotResponse.getPaging().getTotal(), acknowledgedHotspotResponse.getPaging().getTotal());

        //= Tổng số Acknowledge / Tổng số hotspots
        double hotspotReviewedPercentage = securityHotspot == 0 ? 0.0
                : (double) acknowledgedHotspotResponse.getPaging().getTotal() / securityHotspot;

        //Lấy từ tỉ lệ coverage trong tab Measures.Coverage.Coverage
        Double coverage = ofNullable(measureResponse.getComponent())
                .map(ComponentMeasures::getMeasures)
                .stream()
                .flatMap(List::stream)
                .filter(measure -> "coverage".equals(measure.getMetric()))
                .findFirst()
                .map(Measure::getValue)
                .map(Double::parseDouble)
                .map(coveragePercentage -> coveragePercentage / 100.0)
                .orElse(0.0);

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

        //= Tab Measures.Coverage.Lines to Cover - Measures.Coverage.Uncovered Lines
        Integer coveredLine = subtractExact(linesToCover, uncoveredLines);

        return IssueExportData.builder()
                .issues(issueDtos)
                .branch(branchName)
                .securityHotspot(securityHotspot)
                .hotspotReviewed(hotspotReviewedPercentage)
                .coverage(coverage)
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
