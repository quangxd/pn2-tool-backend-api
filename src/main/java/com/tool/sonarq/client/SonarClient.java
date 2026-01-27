package com.tool.sonarq.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tool.sonarq.dto.model.Issue;
import com.tool.sonarq.dto.model.request.ReportRequest;
import com.tool.sonarq.dto.model.response.BranchResponse;
import com.tool.sonarq.dto.model.response.HotspotResponse;
import com.tool.sonarq.dto.model.response.MeasureResponse;
import com.tool.sonarq.dto.model.response.SearchResponse;
import com.tool.sonarq.util.FileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static java.lang.String.format;
import static java.util.Optional.of;
import static reactor.core.publisher.Mono.error;


@Slf4j
@Component
@RequiredArgsConstructor
public class SonarClient {

    @Value("${sonar.timezone}")
    private  String timezone;
    private final WebClient webClient;
    private final FileReader fileReader;
    private final ObjectMapper objectMapper;

    public Mono<SearchResponse> queryIssues(String repository, ReportRequest reportRequest) {
        return webClient.get()
                .uri(uri -> buildQueryIssuesUri(uri, repository, reportRequest))
                .header(HttpHeaders.COOKIE, reportRequest.cookie())
                .exchangeToMono(response -> toSearchResponseMono(response, repository))
                .doOnNext(response -> log.info("Sonar response data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch response for {}: {}", repository, e.getMessage())
                );
    }

    public Mono<BranchResponse> queryBranch(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildQueryBranchUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(this::toBranchResponseMono)
                .doOnNext(response -> log.info("Branch data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch branch for {}: {}", repository, e.getMessage())
                );
    }

    public Mono<MeasureResponse> queryMeasures(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildQueryMeasuresUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(response -> toMeasureResponseMono(response, repository))
                .doOnNext(response -> log.info("Measures data retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch measures for {}: {}", repository, e.getMessage())
                );
    }

    public Mono<HotspotResponse> queryHotspots(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildHotspotsUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(response -> toHotspotResponseMono(response, repository))
                .doOnNext(response -> log.info("Hotspots retrieved for repository: {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch hotspots for {}: {}", repository, e.getMessage())
                );
    }

    private URI buildQueryIssuesUri(UriBuilder uri, String repository, ReportRequest reportRequest) {
        URI issuesUri = uri.path("/api/issues/search")
                .queryParam("components", repository)
                .queryParam("p", reportRequest.pageNumber())
                .queryParam("ps", reportRequest.pageSize())
                .queryParam("issueStatuses", String.join(",", reportRequest.statuses()))
                .queryParam("additionalFields", "_all")
                .queryParam("timeZone", timezone)
                .queryParam("facets", "impactSoftwareQualities,severities,types,impactSeverities,codeVariants")
                .queryParam("s", "FILE_LINE")
                .build();

        log.info("issues search url: {}", issuesUri);
        return issuesUri;
    }

    private URI buildQueryBranchUri(UriBuilder uri, String repository) {
        URI branchUri = uri.path("/api/project_branches/list")
                .queryParam("project", repository)
                .build();

        log.info("project branches url: {}", branchUri);
        return branchUri;
    }

    private URI buildQueryMeasuresUri(UriBuilder uri, String repository) {
        URI measuresUri = uri.path("/api/measures/component")
                .queryParam("component", repository)
                .queryParam("metricKeys", "duplicated_lines_density,uncovered_lines,ncloc,lines_to_cover,coverage")
                .build();

        log.info("measuresUri search url: {}", measuresUri);
        return measuresUri;
    }

    private URI buildHotspotsUri(UriBuilder uri, String repository) {
        URI hotspotUri = uri.path("/api/hotspots/search")
                .queryParam("project", repository)
                .queryParam("status", "TO_REVIEW")
                .queryParam("ps", "500")
                .queryParam("inNewCodePeriod", "false")
                .queryParam("onlyMine", "false")
                .build();

        log.info("hotspots search url: {}", hotspotUri);
        return hotspotUri;
    }

    private Mono<SearchResponse> toSearchResponseMono(ClientResponse response, String repository) {
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
                .map(this::toSearchResponse);
    }

    private SearchResponse toSearchResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, SearchResponse.class);
        } catch (IOException e) {
            log.error("Error while parsing Sonar Issue response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
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

    /**
     * for testing purpose when development to mock response from SonarQ
     * @param repository
     * @param pageSize
     * @return
     */
    public List<Issue> queryIssuesMock(String repository, final int pageSize) {
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
