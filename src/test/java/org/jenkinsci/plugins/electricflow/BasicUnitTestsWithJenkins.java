package org.jenkinsci.plugins.electricflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.Secret;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BasicUnitTestsWithJenkins {
    public static final String FLOW_CONFIG_NAME = "mockFlowConfig";
    public static final String FLOW_PROTOCOL = "https";
    public static final String FLOW_HOST = "localhost";
    public static final String FLOW_PORT = "8443";
    public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
    public static final String FLOW_USER = "user";
    public static final String FLOW_PASSWORD = "password";
    public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

    @Test
    void getConfigurationByDescriptor(JenkinsRule jenkinsRule) {
        ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration = (ElectricFlowGlobalConfiguration) jenkinsRule
                .getInstance()
                .getDescriptorByName("org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

        electricFlowGlobalConfiguration.configurations = new LinkedList<>();

        Configuration configuration = new Configuration(
                FLOW_CONFIG_NAME,
                FLOW_ENDPOINT,
                FLOW_USER,
                Secret.fromString(FLOW_PASSWORD),
                FLOW_REST_API_URI_PATH,
                true,
                false,
                null,
                null);

        electricFlowGlobalConfiguration.configurations.add(configuration);
        electricFlowGlobalConfiguration.save();

        assertEquals(1, electricFlowGlobalConfiguration.getConfigurations().size());
    }

    @Test
    void basicTestFreeStyleProject(JenkinsRule jenkinsRule) throws Exception {
        String command = "echo hello";
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile(command) : new Shell(command));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        File logFile = build.getLogFile();
        StringBuilder log = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(logFile.getPath()))) {
            lines.forEachOrdered(log::append);
            assertTrue(log.toString().contains("echo hello"));
        }
    }
}
