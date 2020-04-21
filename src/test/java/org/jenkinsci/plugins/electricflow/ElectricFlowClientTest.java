package org.jenkinsci.plugins.electricflow;

import java.io.IOException;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ElectricFlowClientTest {
  public static final String FLOW_CONFIG_NAME = "realFlowConfig";
  public static final String FLOW_PROTOCOL = "https";
  public static final String FLOW_HOST = System.getenv("COMMANDER_SERVER");
  public static final String FLOW_PORT = System.getenv("COMMANDER_PORT");
  public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
  public static final String FLOW_USER = System.getenv("COMMANDER_USER");
  public static final String FLOW_PASSWORD = System.getenv("COMMANDER_PASSWORD");
  public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

  @Test
  public void checkConnection() throws IOException {
    /* Test will be skipped if properties are not set */
    Assume.assumeTrue(System.getenv("COMMANDER_PASSWORD") != null);

    getClient().testConnection();
  }

  @Test
  public void releasesWithRunDetails() throws Exception {
    /* Test will be skipped if properties are not set */
    Assume.assumeTrue(System.getenv("COMMANDER_PASSWORD") != null);

    String configuration = FLOW_CONFIG_NAME;
    String projectName = "Default";
    String releaseName = "Application v1.0";

    Release release = getClient().getRelease(configuration, projectName, releaseName);

    assertEquals(release.getReleaseName(), releaseName);
    assertNotNull(release.getFlowRuntimeId());
  }

  @Test
  public void releaseWithoutRunsDetails() throws Exception {
    /* Test will be skipped if properties are not set */
    Assume.assumeTrue(System.getenv("COMMANDER_PASSWORD") != null);

    String configuration = FLOW_CONFIG_NAME;
    String projectName = "Default";
    String releaseName = "Empty";

    Release release = getClient().getRelease(configuration, projectName, releaseName);

    assertEquals(release.getReleaseName(), releaseName);
    assertNull(release.getFlowRuntimeId());
  }

  public static ElectricFlowClient getClient(){
    return new ElectricFlowClient(
        FLOW_ENDPOINT, FLOW_USER, FLOW_PASSWORD,
        FLOW_REST_API_URI_PATH, true
    );
  }

}
