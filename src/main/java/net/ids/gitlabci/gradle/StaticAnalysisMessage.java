package net.ids.gitlabci.gradle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Simple struct that holds analysis output.
 */
@SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Simple structure / bean class")
class StaticAnalysisMessage {

    final String fileName;
    final String tool;

    String method;
    int startLine;
    int endLine;
    int startCol;
    int endCol;

    String message;
    int priority;
    String rule;
    String category;

    String infoUrl;

    StaticAnalysisMessage(final String tool, final String name) {
        this.fileName = name;
        this.tool = tool;
    }

    StaticAnalysisMessage category(final String category) {
        this.category = category;
        return this;
    }

    StaticAnalysisMessage infoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
        return this;
    }

    StaticAnalysisMessage line(final int number) {
        this.startLine = number;
        this.endLine = number;
        return this;
    }

    StaticAnalysisMessage lineAndColumnRange(final int startLine, final int endLine, final int startCol, final int endCol) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.startCol = startCol;
        this.endCol = endCol;
        return this;
    }

    StaticAnalysisMessage lineRange(final int start, final int end) {
        this.startLine = start;
        this.endLine = end;
        return this;
    }

    StaticAnalysisMessage message(final String message) {
        this.message = message;
        return this;
    }

    StaticAnalysisMessage method(final String method) {
        this.method = method;
        return this;
    }

    StaticAnalysisMessage priority(final int priority) {
        this.priority = priority;
        return this;
    }

    StaticAnalysisMessage rule(final String rule) {
        this.rule = rule;
        return this;
    }

}
