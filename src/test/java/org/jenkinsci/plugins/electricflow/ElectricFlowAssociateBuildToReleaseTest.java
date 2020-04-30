package org.jenkinsci.plugins.electricflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class ElectricFlowAssociateBuildToReleaseTest {

  public static final String FLOW_CONFIG_NAME = "realFlowConfig";
  public static final String FLOW_PROTOCOL = "https";
  public static final String FLOW_HOST = System.getenv("COMMANDER_SERVER");
  public static final String FLOW_PORT = System.getenv("COMMANDER_PORT");
  public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
  public static final String FLOW_USER = System.getenv("COMMANDER_USER");
  public static final String FLOW_PASSWORD = System.getenv("COMMANDER_PASSWORD");
  public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

  @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule public JenkinsRule jenkinsRule = new JenkinsRule();

  private static String readLog(InputStream logInputSteam) throws IOException {
    InputStreamReader logFile = new InputStreamReader(logInputSteam);
    StringBuilder log = new StringBuilder();

    final char[] buffer = new char[1024];
    int charsRead;
    while ((charsRead = logFile.read(buffer, 0, buffer.length)) > 0) {
      log.append(buffer, 0, charsRead);
    }

    return log.toString();
  }

  @Test
  public void runPbaWithFreestyleProject() throws Exception {

    String command = "echo hello";
    FreeStyleProject project = jenkinsRule.createFreeStyleProject();

    project
        .getBuildersList()
        .add(Functions.isWindows() ? new BatchFile(command) : new Shell(command));

    ElectricFlowAssociateBuildToRelease pba = new ElectricFlowAssociateBuildToRelease();
    pba.setConfiguration(FLOW_CONFIG_NAME);
    pba.setProjectName("Default");
    pba.setReleaseName("Application v1.0");

    project.getPublishersList().add(pba);

    /* Test will be skipped if properties are not set */
    Assume.assumeTrue(System.getenv("COMMANDER_PASSWORD") != null);
    applyFlowConfiguration(FLOW_CONFIG_NAME);

    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");

    String log = readLog(build.getLogInputStream());
    assertTrue(log.contains("echo hello"));

    // https://34.73.207.2/flow/1b4eaec2-7ef5-11ea-b580-0242ac1a0002/21544765-7ef5-11ea-97da-0242ac1a0002/release/1b4d763e-7ef5-11ea-b580-0242ac1a0002
    assertTrue(log.contains("/flow/#pipeline-run/"));
  }

  @Test
  public void checkPBAGettersAndSetters() {
    ElectricFlowAssociateBuildToRelease pba = new ElectricFlowAssociateBuildToRelease();

    String expectedConfigName = FLOW_CONFIG_NAME;
    String expectedProjectName = "Default";
    String expectedReleaseName = "Application v1.0";

    pba.setConfiguration(expectedConfigName);
    pba.setProjectName(expectedProjectName);
    pba.setReleaseName(expectedReleaseName);

    assertEquals(expectedConfigName, pba.getConfiguration());
    assertEquals(expectedProjectName, pba.getProjectName());
    assertEquals(expectedReleaseName, pba.getReleaseName());
  }

  private void applyFlowConfiguration(String configName) {
    ElectricFlowGlobalConfiguration electricFlowGlobalConfiguration =
        (ElectricFlowGlobalConfiguration)
            jenkinsRule
                .getInstance()
                .getDescriptorByName(
                    "org.jenkinsci.plugins.electricflow.ElectricFlowGlobalConfiguration");

    electricFlowGlobalConfiguration.efConfigurations = new LinkedList<>();

    Configuration configuration =
        new Configuration(
            configName, FLOW_ENDPOINT, FLOW_USER, FLOW_PASSWORD, FLOW_REST_API_URI_PATH, true);

    electricFlowGlobalConfiguration.efConfigurations.add(configuration);
    electricFlowGlobalConfiguration.save();

    assertEquals(1, electricFlowGlobalConfiguration.getConfigurations().size());
  }
}
