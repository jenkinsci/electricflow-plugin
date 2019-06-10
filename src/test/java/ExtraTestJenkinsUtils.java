import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.ParameterizedJobMixIn;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.Future;

public class ExtraTestJenkinsUtils {
    public static <J extends Job<J, R>, R extends Run<J, R>> R buildAndAssertSuccess(final J job, JenkinsRule jenkinsRule) throws Exception {
        return buildAndAssertResult(job, jenkinsRule, Result.SUCCESS);
    }

    public static <J extends Job<J, R>, R extends Run<J, R>> R buildAndAssertFailure(final J job, JenkinsRule jenkinsRule) throws Exception {
        return buildAndAssertResult(job, jenkinsRule, Result.FAILURE);
    }

    public static <J extends Job<J, R>, R extends Run<J, R>> R buildAndAssertResult(final J job, JenkinsRule jenkinsRule, Result result) throws Exception {
        QueueTaskFuture f = new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        }.scheduleBuild2(0);
        @SuppressWarnings("unchecked") // no way to make this compile checked
                Future<R> f2 = f;
        return jenkinsRule.assertBuildStatus(result, f2);
    }
}
