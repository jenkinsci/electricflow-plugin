package org.jenkinsci.plugins.electricflow.extension.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import hudson.model.Result;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.electricflow.extension.CloudBeesFlowPipeline;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CloudBeesFlowPipelineRestTest {

    @Test
    void declarativeStages(JenkinsRule j) throws Exception {
        WorkflowRun b = run(
                j,
                "declarative",
                String.join(
                        "\n",
                        "pipeline {",
                        "  agent none",
                        "  stages {",
                        "    stage('build') { steps { sleep 1 } }",
                        "    stage('skipped') {",
                        "      when { expression { false } }",
                        "      steps { echo 'never' }",
                        "    }",
                        "    stage('tests') {",
                        "      parallel {",
                        "        stage('unit') { steps { sleep 1 } }",
                        "        stage('it') {",
                        "          when { expression { false } }",
                        "          steps { echo 'never' }",
                        "        }",
                        "      }",
                        "    }",
                        "  }",
                        "}"),
                true,
                Result.SUCCESS);

        List<CloudBeesFlowPipeline> stages = CloudBeesFlowPipeline.build(b);
        assertThat(names(stages), contains("build", "skipped", "tests", "unit", "it"));
        Map<String, CloudBeesFlowPipeline> byName = byName(stages);
        assertThat(byName.get("build").getDuration(), greaterThanOrEqualTo(1000L));
        assertThat(byName.get("skipped").getDuration(), is(0L));
        assertThat(byName.get("unit").getDuration(), greaterThanOrEqualTo(1000L));
        assertThat(byName.get("it").getDuration(), is(0L));
        // a parent stage covers its child stages
        assertThat(
                byName.get("tests").getDuration(),
                greaterThanOrEqualTo(byName.get("unit").getDuration()));
        for (CloudBeesFlowPipeline stage : stages) {
            assertThat(stage.getTimestamp(), greaterThanOrEqualTo(b.getStartTimeInMillis()));
        }

        JSONObject json = byName.get("build").toJsonObject();
        assertThat(json.getString("stageName"), is("build"));
        assertThat(json.getLong("duration"), is(byName.get("build").getDuration()));
        assertThat(json.getLong("timestamp"), is(byName.get("build").getTimestamp()));
    }

    @Test
    void nestedScriptedStages(JenkinsRule j) throws Exception {
        WorkflowRun b = run(
                j,
                "nested",
                "stage('outer') { stage('inner1') { sleep 1 }; stage('inner2') { sleep 1 } }; stage('last') { echo 'x' }",
                true,
                Result.SUCCESS);

        List<CloudBeesFlowPipeline> stages = CloudBeesFlowPipeline.build(b);
        assertThat(names(stages), contains("outer", "inner1", "inner2", "last"));
        Map<String, CloudBeesFlowPipeline> byName = byName(stages);
        assertThat(byName.get("inner1").getDuration(), greaterThanOrEqualTo(1000L));
        assertThat(byName.get("inner2").getDuration(), greaterThanOrEqualTo(1000L));
        assertThat(byName.get("outer").getDuration(), greaterThanOrEqualTo(2000L));
    }

    @Test
    void interruptedStages(JenkinsRule j) throws Exception {
        for (Result result : new Result[] {Result.FAILURE, Result.NOT_BUILT, Result.ABORTED}) {
            WorkflowRun b = run(
                    j,
                    "interrupted-" + result,
                    "stage('first') { sleep 1 }\n"
                            + "stage('interrupted') {\n"
                            + "  throw new org.jenkinsci.plugins.workflow.steps.FlowInterruptedException(hudson.model.Result."
                            + result + ")\n"
                            + "}\n"
                            + "stage('never') { echo 'x' }",
                    false,
                    result);

            List<CloudBeesFlowPipeline> stages = CloudBeesFlowPipeline.build(b);
            assertThat(names(stages), contains("first", "interrupted"));
            Map<String, CloudBeesFlowPipeline> byName = byName(stages);
            assertThat(byName.get("first").getDuration(), greaterThanOrEqualTo(1000L));
            assertThat(byName.get("interrupted").getDuration(), greaterThan(0L));
        }
    }

    private static WorkflowRun run(JenkinsRule j, String name, String script, boolean sandbox, Result expected)
            throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, name);
        p.setDefinition(new CpsFlowDefinition(script, sandbox));
        return j.assertBuildStatus(expected, p.scheduleBuild2(0));
    }

    private static List<String> names(List<CloudBeesFlowPipeline> stages) {
        return stages.stream().map(CloudBeesFlowPipeline::getStageName).collect(Collectors.toList());
    }

    private static Map<String, CloudBeesFlowPipeline> byName(List<CloudBeesFlowPipeline> stages) {
        return stages.stream().collect(Collectors.toMap(CloudBeesFlowPipeline::getStageName, Function.identity()));
    }
}
