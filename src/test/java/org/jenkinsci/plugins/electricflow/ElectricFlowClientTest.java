package org.jenkinsci.plugins.electricflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ElectricFlowClientTest {
    public static final String FLOW_CONFIG_NAME = Utils.CONFIG_SKIP_CHECK_CONNECTION;
    public static final String FLOW_PROTOCOL = "https";
    public static final String FLOW_HOST = "localhost";
    public static final String FLOW_PORT = "443";
    public static final String FLOW_ENDPOINT = FLOW_PROTOCOL + "://" + FLOW_HOST + ":" + FLOW_PORT;
    public static final String FLOW_USER = "user";
    public static final String FLOW_PASSWORD = "password";
    public static final String FLOW_REST_API_URI_PATH = "/rest/v1.0";

    private static JenkinsRule jenkinsRule;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @BeforeEach
    void setUp() {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    void createClient() {
        ElectricFlowClient efc =
                new ElectricFlowClient(FLOW_ENDPOINT, FLOW_USER, FLOW_PASSWORD, FLOW_REST_API_URI_PATH, true);

        assertEquals(FLOW_ENDPOINT, efc.getElectricFlowUrl());
    }
}
