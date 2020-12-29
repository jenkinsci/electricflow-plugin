package org.jenkinsci.plugins.electricflow.exceptions;

import org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs.FlowRuntimeResponseData;

public class FlowRuntimeException extends RuntimeException {

  FlowRuntimeResponseData runtimeResponseData;
  String status;

  public FlowRuntimeException(FlowRuntimeResponseData runtimeResponseData) {
    super(runtimeResponseData.toString());

    this.runtimeResponseData = runtimeResponseData;
    this.status = runtimeResponseData.getRuntimeStatus();
  }
}
