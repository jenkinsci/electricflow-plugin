import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.electricflow.*;
import org.jenkinsci.plugins.electricflow.ui.FieldValidationStatus;
import org.jenkinsci.plugins.electricflow.ui.SelectItemValidationWrapper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class BasicUnitTests {
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

        electricFlowGlobalConfiguration.efConfigurations = new LinkedList<>();

        Configuration configuration = new Configuration(FLOW_CONFIG_NAME,
                FLOW_ENDPOINT,
                FLOW_USER,
                FLOW_PASSWORD,
                FLOW_REST_API_URI_PATH,
                true);

        electricFlowGlobalConfiguration.efConfigurations.add(configuration);
        electricFlowGlobalConfiguration.save();

        assertTrue(electricFlowGlobalConfiguration.getConfigurations().size() == 1);
    }

    @Test
    public void testSelectItemValidationWrapper() {
        SelectItemValidationWrapper selectItemValidationWrapper = new SelectItemValidationWrapper(
                FieldValidationStatus.OK,
                "validation message",
                "value");
        System.out.println(selectItemValidationWrapper.getJsonStr());
        String selectItemValidationWrapperJsonStr = selectItemValidationWrapper.getJsonStr();

        assertTrue(Pattern
                .compile(".*\"validationMessage\":\"validation message\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern
                .compile(".*\"validationStatus\":\"OK\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
        assertTrue(Pattern
                .compile(".*\"value\":\"value\".*", Pattern.DOTALL)
                .matcher(selectItemValidationWrapperJsonStr)
                .matches());
    }

    @Test
    public void checkValidateValueOnEmpty() {
        FormValidation formValidation;

        formValidation = Utils.validateValueOnEmpty("", "Field name");
        assertTrue(formValidation.getMessage().equals("Field name field should not be empty"));
        assertTrue(formValidation.kind == FormValidation.Kind.WARNING);

        formValidation = Utils.validateValueOnEmpty("test", "Field name");
        assertTrue(formValidation.getMessage().equals("Field name field should not be empty"));
        assertTrue(formValidation.kind == FormValidation.Kind.OK);
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
