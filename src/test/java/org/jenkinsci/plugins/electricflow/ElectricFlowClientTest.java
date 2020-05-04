package org.jenkinsci.plugins.electricflow;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ElectricFlowClientTest {
  public static final String FLOW_CONFIG_NAME = Utils.CONFIG_SKIP_CHECK_CONNECTION;
  public static final String FLOW_PROTOCOL = "https";
  public static final String FLOW_HOST = "localhost";
  public static final String FLOW_PORT = "443";
  public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
  public static final String FLOW_USER = "user";
  public static final String FLOW_PASSWORD = "password";
  public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  @Before
  public void setUp() {
    jenkinsRule.getInstance().getInjector().injectMembers(this);
  }

  @Test
  public void createClient() {
    ElectricFlowClient efc =
        new ElectricFlowClient(
            FLOW_ENDPOINT, FLOW_USER, FLOW_PASSWORD, FLOW_REST_API_URI_PATH, true);

    assertEquals(efc.getElectricFlowUrl(), FLOW_ENDPOINT);
  }
}
