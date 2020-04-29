import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.Secret;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import static org.junit.Assert.assertTrue;

public class BasicUnitTestsWithJenkins {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    public static final String FLOW_CONFIG_NAME = "mockFlowConfig";
    public static final String FLOW_PROTOCOL = "https";
    public static final String FLOW_HOST = "localhost";
    public static final String FLOW_PORT = "8443";
    public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
    public static final String FLOW_USER = "user";
    public static final String FLOW_PASSWORD = "password";
    public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

    @Test
    public void getConfigurationByDescriptor() {
        ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration = (ElectricFlowGlobalConfiguration) jenkinsRule.getInstance().getDescriptorByName("org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

        electricFlowGlobalConfiguration.configurations = new LinkedList<>();

        Configuration configuration = new Configuration(FLOW_CONFIG_NAME,
                FLOW_ENDPOINT,
                FLOW_USER,
                Secret.fromString(FLOW_PASSWORD),
                FLOW_REST_API_URI_PATH,
                true);

        electricFlowGlobalConfiguration.configurations.add(configuration);
        electricFlowGlobalConfiguration.save();

        assertTrue(electricFlowGlobalConfiguration.getConfigurations().size() == 1);
    }

    @Test
    public void basicTestFreeStyleProject() throws Exception {
        String command = "echo hello";
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile(command) : new Shell(command));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        File logFile = build.getLogFile();
        StringBuilder log = new StringBuilder();
        Files.lines(Paths.get(logFile.getPath())).forEachOrdered(log::append);
        assertTrue(log.toString().contains("echo hello"));
    }
}
