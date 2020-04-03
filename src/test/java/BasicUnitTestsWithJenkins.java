import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import org.jenkinsci.plugins.electricflow.Configuration;
import org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class BasicUnitTestsWithJenkins {
  public static final String FLOW_CONFIG_NAME = "mockFlowConfig";
  public static final String FLOW_PROTOCOL = "https";
  public static final String FLOW_HOST = "localhost";
  public static final String FLOW_PORT = "8443";
  public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
  public static final String FLOW_USER = "user";
  public static final String FLOW_PASSWORD = "password";
  public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";
  @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void getConfigurationByDescriptor() {
    ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration =
        (ElectricFlowGlobalConfiguration)
            jenkinsRule
                .getInstance()
                .getDescriptorByName(
                    "org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

    electricFlowGlobalConfiguration.efConfigurations = new LinkedList<>();

    Configuration configuration =
        new Configuration(
            FLOW_CONFIG_NAME,
            FLOW_ENDPOINT,
            FLOW_USER,
            FLOW_PASSWORD,
            FLOW_REST_API_URI_PATH,
            true);

    electricFlowGlobalConfiguration.efConfigurations.add(configuration);
    electricFlowGlobalConfiguration.save();

    assertEquals(1, electricFlowGlobalConfiguration.getConfigurations().size());
  }

  @Test
  public void basicTestFreeStyleProject() throws Exception {
    String command = "echo hello";
    FreeStyleProject project = jenkinsRule.createFreeStyleProject();
    project
        .getBuildersList()
        .add(Functions.isWindows() ? new BatchFile(command) : new Shell(command));
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");

    InputStreamReader logFile = new InputStreamReader(build.getLogInputStream());
    StringBuilder log = new StringBuilder();

    final char[] buffer = new char[1024];
    int charsRead;
    while((charsRead = logFile.read(buffer, 0, buffer.length)) > 0){
      log.append(buffer, 0, charsRead);
    }

    assertTrue(log.toString().contains("echo hello"));
  }
}
