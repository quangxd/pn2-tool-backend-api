package com.tool.sonarq.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tool.sonarq.dto.*;
import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.dto.response.BranchResponse;
import com.tool.sonarq.dto.response.HotspotResponse;
import com.tool.sonarq.dto.response.MeasureResponse;
import com.tool.sonarq.dto.response.SearchResponse;
import com.tool.sonarq.util.FileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.subtractExact;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.zip;


@Slf4j
@Component
@RequiredArgsConstructor
public class SonarClient {

    @Value("${sonar.timezone}")
    private  String timezone;
    private final WebClient webClient;
    private final FileReader fileReader;
    private final ObjectMapper objectMapper;

    public Mono<IssueExportData> fetchAllIssues(String repository, ReportRequest reportRequest) {
        return zip(
                queryIssues(repository, reportRequest),
                queryBranch(repository, reportRequest.cookie()),
                queryMeasures(repository, reportRequest.cookie()),
                queryHotspots(repository, reportRequest.cookie())
        ).map(tuples ->
                assembleExportData(tuples.getT1(), tuples.getT2(), tuples.getT3(), tuples.getT4()));
    }

    private Mono<SearchResponse> queryIssues(String repository, ReportRequest reportRequest) {
        return webClient.get()
                .uri(uri -> buildQueryIssuesUri(uri, repository, reportRequest))
                .header(HttpHeaders.COOKIE, reportRequest.cookie())
                .exchangeToMono(response -> toSonarResponseMono(response, repository))
                .doOnNext(response -> log.info("Sonar response data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch response for {}: {}", repository, e.getMessage())
                );
    }

    private Mono<BranchResponse> queryBranch(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildQueryBranchUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(this::toBranchResponseMono)
                .doOnNext(response -> log.info("Branch data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch branch for {}: {}", repository, e.getMessage())
                );
    }

    private Mono<MeasureResponse> queryMeasures(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildQueryMeasuresUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(response -> toMeasureResponseMono(response, repository))
                .doOnNext(response -> log.info("Measures data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch measures for {}: {}", repository, e.getMessage())
                );
    }

    private Mono<HotspotResponse> queryHotspots(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildHotspotsUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(response -> toHotspotResponseMono(response, repository))
                .doOnNext(response -> log.info("Hotspots retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch hotspots for {}: {}", repository, e.getMessage())
                );
    }

    private IssueExportData assembleExportData(SearchResponse searchResponse, BranchResponse branchResponse,
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

    private Mono<SearchResponse> toSonarResponseMono(ClientResponse response, String repository) {
        if (response.statusCode().isError()) {
            return response
                    .createException()
                    .flatMap(Mono::error);
        }

        return response.bodyToMono(byte[].class)
                .doOnNext(bytes ->
                        log.info(
                                "Sonar response size = {} KB, repository = {}",
                                bytes.length / 1024,
                                repository
                        )
                )
                .switchIfEmpty(error(
                        new IllegalStateException("Sonar response is empty for repository " + repository)
                ))
                .map(this::toSonarResponse);
    }

    private Mono<BranchResponse> toBranchResponseMono(ClientResponse response) {
        if (response.statusCode().isError()) {
            return response
                    .createException()
                    .flatMap(Mono::error);
        }

        return response.bodyToMono(byte[].class)
                .doOnNext(bytes ->
                        log.info(
                                "Branch response size = {} KB, length = {}",
                                bytes.length / 1024,
                                bytes.length
                        )
                )
                .switchIfEmpty(error(
                        new IllegalStateException("Branch response is empty")
                ))
                .map(this::toBranchResponse);
    }

    private SearchResponse toSonarResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, SearchResponse.class);
        } catch (IOException e) {
            log.error("Error while parsing Sonar Issue response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private BranchResponse toBranchResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, BranchResponse.class);
        } catch (IOException e) {
            log.error("Error while parsing Branch response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Mono<MeasureResponse> toMeasureResponseMono(ClientResponse response, String repository) {
        if (response.statusCode().isError()) {
            return response
                    .createException()
                    .flatMap(Mono::error);
        }
        return response.bodyToMono(byte[].class)
                .doOnNext(bytes -> log.info("Sonar Measures response size = {} KB, repository = {}",
                        bytes.length / 1024, repository))
                .switchIfEmpty(
                        error(new IllegalStateException("Sonar Measures response is empty for repository " + repository))
                )
                .map(this::toMeasureResponse);
    }

    private MeasureResponse toMeasureResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, MeasureResponse.class);
        } catch (IOException e) {
            log.error("Error while parsing Sonar Measure response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Mono<HotspotResponse> toHotspotResponseMono(ClientResponse response, String repository) {
        if (response.statusCode().isError()) {
            return response.createException().flatMap(Mono::error);
        }
        return response.bodyToMono(byte[].class)
                .doOnNext(bytes -> log.info("Sonar Hotspot response size = {} KB, repository = {}",
                        bytes.length / 1024, repository))
                .switchIfEmpty(
                        error(new IllegalStateException("Sonar Hotspot response is empty for repository " + repository))
                )
                .map(this::toHotspotResponse);
    }

    private HotspotResponse toHotspotResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, HotspotResponse.class);
        } catch (IOException e) {
            log.error("Error while parsing Sonar Hotspot response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
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

    private URI buildQueryIssuesUri(UriBuilder uri, String repository, ReportRequest reportRequest) {
        return uri.path("/api/issues/search")
                .queryParam("components", repository)
                .queryParam("p", reportRequest.pageNumber())
                .queryParam("ps", reportRequest.pageSize())
                .queryParam("issueStatuses", String.join(",", reportRequest.statuses()))
                .queryParam("additionalFields", "_all")
                .queryParam("timeZone", timezone)
                .queryParam(
                        "facets",
                        "impactSoftwareQualities,severities,types,impactSeverities,codeVariants"
                )
                .queryParam("s", "FILE_LINE")
                .build();
    }

    private URI buildQueryBranchUri(UriBuilder uri, String repository) {
        return uri.path("/api/project_branches/list")
                .queryParam("project", repository)
                .build();
    }

    private URI buildQueryMeasuresUri(UriBuilder uri, String repository) {
        return uri.path("/api/measures/component")
                .queryParam("component", repository)
                .queryParam(
                        "metricKeys",
                        "duplicated_lines_density,uncovered_lines,ncloc,lines_to_cover,coverage")
                .build();
    }

    private URI buildHotspotsUri(UriBuilder uri, String repository) {
        return uri.path("/api/hotspots/search")
                .queryParam("project", repository)
                .queryParam("status", "TO_REVIEW")
                .queryParam("ps", "500")
                .queryParam("inNewCodePeriod", "false")
                .build();
    }

    /**
     * for testing purpose when development to mock response from SonarQ
     * @param repository
     * @param pageSize
     * @return
     */
    public List<Issue> fetchAllIssuesMock(String repository, final int pageSize) {
        List<Issue> issues = new ArrayList<>();
        try {
            issues = of(
                    objectMapper.readValue(
                            fileReader.getResponse(format("%s.json", repository)),
                            SearchResponse.class)
            ).map(SearchResponse::getIssues)
                    .orElseGet(List::of);

        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        return  issues;
    }
}
