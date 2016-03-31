package net.ids.gitlabci.gradle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Simple struct that holds JUnit output.
 */
@SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Simple structure / bean class")
class JUnitTestCase {
    // We need a unique ID for each test case to simplify output rendering
    static int uniqueIdSource;

    final int uniqueId;
    final String testName;
    final String className;
    final float time;
    String message;
    String type;
    String error;

    JUnitTestCase(final String testName, final String className, final float time) {
        this.uniqueId = uniqueIdSource++;

        this.testName = testName;
        this.className = className;
        this.time = time;
    }

    void failure(final String message, final String type, final String error) {
        this.message = message;
        this.type = type;
        this.error = error;
    }
}
