# gitlabci-gradle-analysis
Post-build analysis for gitlabci and gradle

Jenkins provides several plugins that post-process the build output and integrate these artefacts (static code analysis, test output) into the UI. 

GitLab CI does not provide the same functionality, so this project is a simple (read, expedient) alternative that processes the XML output from static analysis tools (Checkstyle, Findbugs and PMD) and testing frameworks (Junit).
