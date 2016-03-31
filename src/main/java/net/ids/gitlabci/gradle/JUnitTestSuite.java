package net.ids.gitlabci.gradle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Simple struct that holds JUnit output.
 */
@SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Simple structure / bean class")
class JUnitTestSuite {
    // We need a unique ID for each test suite to simplify output rendering
    static int uniqueIdSource;

    final int uniqueId;
    final String suiteName;
    final int testCount;
    final int skippedCount;
    final int failureCount;
    final int errorCount;
    final float time;
    final String stdout;
    final String stderr;

    JUnitTestSuite(final String suiteName, final int testCount, final int skippedCount, final int failureCount, final int errorCount, final float time, final String stdout, final String stderr) {
        this.uniqueId = uniqueIdSource++;

        this.suiteName = suiteName;
        this.testCount = testCount;
        this.skippedCount = skippedCount;
        this.failureCount = failureCount;
        this.errorCount = errorCount;
        this.time = time;
        this.stdout = stdout;
        this.stderr = stderr;
    }
}
