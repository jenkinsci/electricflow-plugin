package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

public interface FlowRuntimeResponseData {

    Boolean isCompleted();

    String getRuntimeOutcome();

    String getRuntimeStatus();

    String getRuntimeId();
}
