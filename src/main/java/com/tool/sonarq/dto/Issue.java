package com.tool.sonarq.dto;

import lombok.Data;

import java.util.List;

@Data
public class Issue {
    private String key;
    private String rule;
    private String severity;
    private String component;
    private String project;
    private int line;
    private String hash;
    private TextRange textRange;
    private List<Flow> flows;
    private String status;
    private String message;
    private String effort;
    private String debt;
    private String author;
    private List<String> tags;
    private List<String> transitions;
    private List<String> actions;
    private List<Object> comments;
    private String creationDate;
    private String updateDate;
    private String type;
    private String scope;
    private boolean quickFixAvailable;
    private List<Object> messageFormattings;
    private List<Object> codeVariants;
    private String cleanCodeAttribute;
    private String cleanCodeAttributeCategory;
    private List<Impact> impacts;
    private String issueStatus;
    private boolean prioritizedRule;
    private boolean fromSonarQubeUpdate;
    private List<String> internalTags;
    private String linkedTicketStatus;

}

@Data
class TextRange {
    private int startLine;
    private int endLine;
    private int startOffset;
    private int endOffset;
}

@Data
class Flow {
    private List<Location> locations;
}

@Data
class Location {
    private String component;
    private TextRange textRange;
    private String msg;
    private List<Object> msgFormattings;
}

