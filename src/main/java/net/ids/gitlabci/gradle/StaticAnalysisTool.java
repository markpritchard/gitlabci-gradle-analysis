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

import org.joox.JOOX;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.jknack.handlebars.Template;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Scans the nominated root directory looking for output from static analysis tools and merges them into a single HTML report.
 * <p>
 * Currently supports checkstyle, findbugs and PMD.
 */
public class StaticAnalysisTool {

    private static final Logger LOG = LoggerFactory.getLogger(StaticAnalysisTool.class);

    private static final Template OUTPUT_TEMPLATE = HandlebarsUtil.compile(StaticAnalysisTool.class.getResourceAsStream("output.hbs"));

    /**
     * Parses the output from the static analysis into a simple Java class for later merging / rendering.
     */
    private static void parse(final Path path, final List<AnalysisMessage> messages) {
        try {
            // Parse the analysis tool output
            Document document = $(path.toFile()).document();

            // Process based on the type of static analysis
            final String type = path.getName(path.getNameCount() - 2).toString();
            switch (type) {
                case "checkstyle":
                    parseCheckstyle(document, messages);
                    break;

                case "findbugs":
                    parseFindbugs(document, messages);
                    break;

                case "pmd":
                    parsePMD(document, messages);
                    break;

                default:
                    throw new IllegalStateException("Unknown static analysis type: " + type);
            }
        } catch (SAXException | IOException e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Handles checkstyle output XML.
     * <p>
     * Its a fairly straightforward format with each error instance located under /checkstyle/file/error.
     */
    private static void parseCheckstyle(final Document document, final List<AnalysisMessage> messages) {
        $(document).xpath("//checkstyle/file").map(JOOX::$).forEach(file -> {
            final String fileName = file.attr("name");

            file.find("error").map(JOOX::$).forEach(error -> {
                final int lineNumber = Integer.parseInt(error.attr("line"));
                final String messageText = error.attr("message");
                final String severityText = error.attr("severity");
                final String source = "config_" + error.attr("source").replace("com.puppycrawl.tools.checkstyle.checks.", "").replace("Check", "").replace(".", ".html#");
                final String infoUrl = "http://checkstyle.sourceforge.net/" + source;
                final int priority;
                switch (severityText) {
                    case "error":
                        priority = 1;
                        break;
                    case "warning":
                        priority = 2;
                        break;
                    case "info":
                        priority = 3;
                        break;
                    default:
                        throw new IllegalStateException("Unknown checkstyle severity text: " + severityText);
                }
                final String rule = error.attr("source");

                final AnalysisMessage message = new AnalysisMessage("checkstyle", fileName)
                        .line(lineNumber)
                        .message(messageText)
                        .priority(priority)
                        .rule(rule)
                        .category("style")
                        .infoUrl(infoUrl);
                messages.add(message);
            });
        });
    }

    /**
     * Parses a Findbugs XML output file.
     * <p>
     * The format is a little more complex, with each instance of a bug under /BugCollection/BugInstance.
     * The same error, in the same class will be present as two separate /BugCollection/BugInstance elements.
     * Each BugInstance can have Class, Class/SourceLine, Method, Method/SourceLine, SourceLine elements.
     * The SourceLine range narrows as you proceed down the hierarchy.
     */
    private static void parseFindbugs(final Document document, final List<AnalysisMessage> messages) {
        $(document).xpath("//BugCollection/BugInstance").map(JOOX::$).forEach(bug -> {
            // Grab information from the BugInstance element
            final int priority = Integer.parseInt(bug.attr("priority"));
            final String rule = bug.attr("type");
            final String infoUrl = "http://findbugs.sourceforge.net/bugDescriptions.html#" + rule;
            final String category = bug.attr("category").toLowerCase().replace('_', ' ');

            // Collect information from the Class child element (we default the line range to that for the class and narrow it as we find lines below)
            final Match classMatch = bug.child("Class");
            final String fileName = classMatch.attr("classname");
            final Match classSourceLineMatch = classMatch.child("SourceLine");
            int startLine = Integer.parseInt(classSourceLineMatch.attr("start"));
            int endLine = Integer.parseInt(classSourceLineMatch.attr("end"));

            // Prepare a message for the class
            final AnalysisMessage message = new AnalysisMessage("findbugs", fileName)
                    .lineRange(startLine, endLine)
                    .priority(priority)
                    .rule(rule)
                    .category(category)
                    .infoUrl(infoUrl);

            // A BugInstance can have a Method child element
            final Match methodMatch = bug.child("Method");
            if (methodMatch.isNotEmpty()) {
                message.method(methodMatch.attr("name"));

                // A Method element can have a SourceLine child too
                final Match sourceLineMatch = methodMatch.child("SourceLine");
                if (sourceLineMatch.isNotEmpty()) {
                    message.lineRange(Integer.parseInt(sourceLineMatch.attr("start")), Integer.parseInt(sourceLineMatch.attr("end")));
                }
            }

            // A BugInstance can also have a direct SourceLine element
            final Match sourceLineMatch = bug.child("SourceLine");
            if (sourceLineMatch.isNotEmpty()) {
                message.lineRange(Integer.parseInt(sourceLineMatch.attr("start")), Integer.parseInt(sourceLineMatch.attr("end")));
            }

            messages.add(message);
        });
    }

    private static void parsePMD(final Document document, final List<AnalysisMessage> messages) {
        $(document).xpath("//pmd/file").map(JOOX::$).forEach(file -> {
            final String fileName = file.attr("name");

            file.find("violation").map(JOOX::$).forEach(violation -> {
                final int beginLine = Integer.parseInt(violation.attr("beginline"));
                final int endLine = Integer.parseInt(violation.attr("endline"));
                final int beginCol = Integer.parseInt(violation.attr("begincolumn"));
                final int endCol = Integer.parseInt(violation.attr("endcolumn"));
                final String rule = violation.attr("rule");
                final String category = violation.attr("ruleset").toLowerCase();
                final String method = violation.attr("method");
                final String infoUrl = violation.attr("externalInfoUrl");
                final int priority = Integer.parseInt(violation.attr("priority"));

                final AnalysisMessage message = new AnalysisMessage("pmd", fileName)
                        .lineAndColumnRange(beginLine, endLine, beginCol, endCol)
                        .method(method)
                        .priority(priority)
                        .rule(rule)
                        .category(category)
                        .infoUrl(infoUrl);
                messages.add(message);
            });
        });
    }

    private static boolean run(final String rootDir, final String outputFile) throws IOException {
        final List<AnalysisMessage> messages = Lists.newArrayList();

        // Find all relevant static analysis output
        Files.walk(Paths.get(rootDir))
                .forEach(path -> {
                    final String fileName = path.getFileName().toString();

                    // Ignore if not a file or not in the expected subdirectory or not the expected name
                    if (Files.isRegularFile(path) && path.toString().contains("/build/reports/") && ("main.xml".equals(fileName) || "test.xml".equals(fileName))) {
                        LOG.info("Processing file {}", path);
                        parse(path, messages);
                    }
                });

        if (messages.isEmpty()) {
            LOG.info("No static analysis errors found.");
            return true;
        } else {
            LOG.error("Static analysis reported {} errors.", messages.size());

            // Render the output file
            final Map<String, Object> model = Maps.newHashMap();
            model.put("messages", messages);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8)) {
                HandlebarsUtil.render(OUTPUT_TEMPLATE, model, writer);
                LOG.info("Wrote report to {}", outputFile);
            }

            // Not OK
            return false;
        }
    }

    /**
     * Command-line entry point to the static analysis scanner.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("USAGE: StaticAnalysisTool <root-dir> <output-file>");
            System.exit(1);
        }

        if (!run(args[0], args[1])) {
            System.exit(1);
        }
    }
}
