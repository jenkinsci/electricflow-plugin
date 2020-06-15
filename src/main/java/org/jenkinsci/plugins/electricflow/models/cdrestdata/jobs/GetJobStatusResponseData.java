package org.jenkinsci.plugins.electricflow.models.cdrestdata.jobs;

import static org.jenkinsci.plugins.electricflow.Utils.formatJsonOutput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetJobStatusResponseData {

  @JsonProperty() private String jobId;
  @JsonProperty() private CdJobStatus status;
  @JsonProperty() private CdJobOutcome outcome;
  @JsonRawValue() private String content;

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public CdJobStatus getStatus() {
    return status;
  }

  public void setStatus(CdJobStatus status) {
    this.status = status;
  }

  public CdJobOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(CdJobOutcome outcome) {
    this.outcome = outcome;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    if (getStatus() == CdJobStatus.unknown || getOutcome() == CdJobOutcome.unknown) {
      try {
        return "CD Job Status Response (unexpected json): " + formatJsonOutput(getContent());
      } catch (IOException e) {
        return "CD Job Status Response (unexpected content): " + getContent();
      }
    }
    return "CD Job Status Response Data: {"
        + "jobId="
        + jobId
        + ", status="
        + status
        + ", outcome="
        + outcome
        + '}';
  }
}
