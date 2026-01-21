package com.tool.sonarq.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tool.sonarq.dto.Branch;
import com.tool.sonarq.dto.Issue;
import com.tool.sonarq.dto.request.ReportRequest;
import com.tool.sonarq.dto.response.BranchResponse;
import com.tool.sonarq.dto.response.IssueExportData;
import com.tool.sonarq.dto.response.SonarResponse;
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
                queryBranch(repository, reportRequest.cookie())
        ).map(tuples ->
                assembleExportData(tuples.getT1(), tuples.getT2()));
    }

    private Mono<List<Issue>> queryIssues(String repository, ReportRequest reportRequest) {
        return webClient.get()
                .uri(uri -> buildQueryIssuesUri(uri, repository, reportRequest))
                .header(HttpHeaders.COOKIE, reportRequest.cookie())
                .exchangeToMono(response -> toSonarResponseMono(response, repository))
                .doOnNext(response -> log.info("Sonar response data retrieved for repository: {}", repository))
                .map(this::toIssues)
                .doOnError(
                        e -> log.error("Failed to fetch response for {}: {}", repository, e.getMessage())
                );
    }

    private Mono<BranchResponse> queryBranch(String repository, String cookie) {
        return webClient.get()
                .uri(uri -> buildQueryBranchUri(uri, repository))
                .header(HttpHeaders.COOKIE, cookie)
                .exchangeToMono(this::toBranchResponseMono)
                .doOnNext(response -> log.info("Branch data retrieved for {}", repository))
                .doOnError(
                        e -> log.error("Failed to fetch branch for {}: {}", repository, e.getMessage())
                );
    }

    private IssueExportData assembleExportData(List<Issue> issues, BranchResponse branchResponse) {
        String branchName = ofNullable(branchResponse.getBranches())
                .flatMap(list -> list.stream().findFirst())
                .map(Branch::getName)
                .orElse(EMPTY);

        return IssueExportData.builder()
                .issues(issues)
                .branch(branchName)
                .build();
    }

    private Mono<SonarResponse> toSonarResponseMono(ClientResponse response, String repository) {
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

    private SonarResponse toSonarResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, SonarResponse.class);
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

    private List<Issue> toIssues(SonarResponse sonarResponse) {
        return ofNullable(sonarResponse.getIssues())
                .orElseGet(List::of);
    }

    private URI buildQueryIssuesUri(
            UriBuilder uri,
            String repository,
            ReportRequest reportRequest
    ) {
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
                            SonarResponse.class)
            ).map(SonarResponse::getIssues)
                    .orElseGet(List::of);

        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        return  issues;
    }
}
