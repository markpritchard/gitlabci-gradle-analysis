package net.ids.gitlabci.gradle;

import static org.joox.JOOX.$;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joox.JOOX;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.github.jknack.handlebars.Template;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.ids.util.HandlebarsUtil;

/**
 * Scans the nominated root directory looking for output from JUnit and merges it into a single HTML report.
 */
public class JUnitAnalysisTool {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitAnalysisTool.class);

    private static final Template OUTPUT_TEMPLATE = HandlebarsUtil.compile(JUnitAnalysisTool.class.getResourceAsStream("junit-output.hbs"));

    /**
     * Parses the output from the static analysis into a simple Java class for later merging / rendering.
     */
    private static void parse(final Path path, final List<Pair<JUnitTestSuite, List<JUnitTestCase>>> results) {
        try {
            // Extract suite level info
            Match suite = $(path.toFile());
            final String suiteName = suite.attr("name");
            final int testCount = Integer.parseInt(suite.attr("tests"));
            final int skippedCount = Integer.parseInt(suite.attr("skipped"));
            final int failureCount = Integer.parseInt(suite.attr("failures"));
            final int errorCount = Integer.parseInt(suite.attr("errors"));
            final float suiteTime = Float.parseFloat(suite.attr("time"));
            final String stdout = suite.child("system-out").content();
            final String stderr = suite.child("system-err").content();
            final JUnitTestSuite suiteInfo = new JUnitTestSuite(suiteName, testCount, skippedCount, failureCount, errorCount, suiteTime, stdout, stderr);

            // Extract each test case
            final List<JUnitTestCase> tests = Lists.newArrayList();
            $(suite).children("testcase").map(JOOX::$).forEach(testCase -> {
                final String testName = testCase.attr("name");
                final String className = testCase.attr("classname");
                final float testTime = Float.parseFloat(testCase.attr("time"));
                final JUnitTestCase testInfo = new JUnitTestCase(testName, className, testTime);

                final Match failure = testCase.child("failure");
                if (failure.isNotEmpty()) {
                    final String message = failure.attr("message");
                    final String type = failure.attr("type");
                    final String error = failure.content();
                    testInfo.failure(message, type, error);
                }

                tests.add(testInfo);
            });

            // Emit this test suite + cases
            results.add(Pair.of(suiteInfo, tests));
        } catch (SAXException | IOException e) {
            Throwables.propagate(e);
        }
    }

    private static boolean run(final String rootDir, final String outputFile) throws IOException {
        final List<Pair<JUnitTestSuite, List<JUnitTestCase>>> results = Lists.newArrayList();

        // Find all relevant JUnit output
        Files.walk(Paths.get(rootDir))
                .forEach(path -> {
                    final String fileName = path.getFileName().toString();

                    // Ignore if not a file or not in the expected subdirectory or not the expected name
                    if (Files.isRegularFile(path) && path.toString().contains("/build/test-results/") && fileName.startsWith("TEST-") && fileName.endsWith(".xml")) {
                        LOG.info("Processing file {}", path);
                        parse(path, results);
                    }
                });

        if (results.isEmpty()) {
            // We should have found some test output
            LOG.error("No JUnit test files found.");
            return false;
        } else {
            // Sort the results
            sort(results);

            // Render the output file
            final Map<String, Object> model = Maps.newHashMap();
            model.put("results", results);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8)) {
                HandlebarsUtil.render(OUTPUT_TEMPLATE, model, writer);
                LOG.info("Wrote report to {}", outputFile);
            }

            // Count the number of failures
            int failCount = 0;
            for (Pair<JUnitTestSuite, List<JUnitTestCase>> result : results) {
                failCount += result.getLeft().errorCount + result.getLeft().failureCount;
            }

            // Return appropriately
            if (failCount > 0) {
                LOG.error("JUnit run reported {} errors.", results.size());
                return false;
            } else {
                LOG.info("JUnit run reported 0 errors.");
                return true;
            }
        }
    }

    private static void sort(final List<Pair<JUnitTestSuite, List<JUnitTestCase>>> results) {
        // Order the results by fail/pass then by suite name
        results.sort((left, right) -> {
            // Sort failures first
            int comparison = Boolean.compare(right.getLeft().hasFailures(), left.getLeft().hasFailures());
            if (comparison == 0) {
                comparison = left.getLeft().suiteName.compareTo(right.getLeft().suiteName);
            }

            return comparison;
        });

        // Now sort each of the tests by fail / pass then name
        results.stream().map(Pair::getRight).forEach(testCases -> testCases.sort((left, right) -> {
            int comparison = Boolean.compare(StringUtils.isEmpty(left.error), StringUtils.isEmpty(right.error));
            if (comparison == 0) {
                comparison = left.testName.compareTo(right.testName);
            }

            return comparison;
        }));
    }

    /**
     * Command-line entry point to the JUnit scanner.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("USAGE: JUnitAnalysisTool <root-dir> <output-file>");
            System.exit(1);
        }

        if (!run(args[0], args[1])) {
            System.exit(1);
        }
    }
}
