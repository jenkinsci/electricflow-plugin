// Configuration.java --
//
// Configuration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jenkinsci.plugins.electricflow.credentials.ItemCredentialHandler;
import org.jenkinsci.plugins.electricflow.factories.ElectricFlowClientFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/** Configuration to access ElectricFlow server. */
public class Configuration extends AbstractDescribableImpl<Configuration> {

  private static final Log log = LogFactory.getLog(Configuration.class);

  private final String configurationName;
  private final String electricFlowUser;
  private final Secret electricFlowPassword;
  private final String electricFlowUrl;
  private final String electricFlowApiVersion;
  private final boolean ignoreSslConnectionErrors;
  private final boolean doNotSendBuildDetails;
  private final Credential overrideCredential;
  private final String credsType;

  // ~ Constructors -----------------------------------------------------------

  @DataBoundConstructor
  public Configuration(
      String configurationName,
      String electricFlowUrl,
      String electricFlowUser,
      Secret electricFlowPassword,
      String electricFlowApiVersion,
      boolean ignoreSslConnectionErrors,
      boolean doNotSendBuildDetails,
      String credentialId,
      String credsType) {
    this.configurationName = configurationName;
    // Removing trailing slashes if any.
    electricFlowUrl = electricFlowUrl.replaceAll("/+$", "");
    this.electricFlowUrl = electricFlowUrl;
    this.electricFlowApiVersion = electricFlowApiVersion;
    this.ignoreSslConnectionErrors = ignoreSslConnectionErrors;
    this.doNotSendBuildDetails = doNotSendBuildDetails;
    this.credsType = credsType;
    if (credsType != null && credsType.equals("storedCreds")) {
      this.overrideCredential = new Credential(credentialId);
      this.electricFlowUser = null;
      this.electricFlowPassword = null;
    } else {
      this.electricFlowUser = electricFlowUser;
      this.electricFlowPassword = electricFlowPassword;
      this.overrideCredential = null;
    }
  }

  // ~ Methods ----------------------------------------------------------------

  public String getConfigurationName() {
    return this.configurationName;
  }

  public String getElectricFlowApiVersion() {
    return this.electricFlowApiVersion;
  }

  public boolean getIgnoreSslConnectionErrors() {
    return this.ignoreSslConnectionErrors;
  }

  public boolean getDoNotSendBuildDetails() {
    return this.doNotSendBuildDetails;
  }

  public Secret getElectricFlowPassword() {
    return this.electricFlowPassword;
  }

  public String getElectricFlowUrl() {
    return this.electricFlowUrl;
  }

  public String getElectricFlowUser() {
    return this.electricFlowUser;
  }

  public Credential getOverrideCredential() {
    return overrideCredential;
  }

  public String getCredentialId() {
    if (getOverrideCredential() == null) {
      return null;
    }
    return getOverrideCredential().getCredentialId();
  }

  public String getCredsType() {
    return credsType;
  }

  // ~ Inner Classes ----------------------------------------------------------

  @Extension
  public static final class ConfigurationDescriptor extends Descriptor<Configuration> {

    // ~ Methods ------------------------------------------------------------

    public FormValidation doCheckConfigurationName(@QueryParameter String value) {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      return Utils.validateValueOnEmpty(value, "Configuration name");
    }

    public FormValidation doCheckElectricFlowApiVersion(@QueryParameter String value) {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      return Utils.validateValueOnEmpty(value, "CloudBees CD api version");
    }

    public FormValidation doCheckElectricFlowPassword(@QueryParameter String value) {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      return Utils.validateValueOnEmpty(value, "CloudBees CD password");
    }

    public FormValidation doCheckElectricFlowUrl(@QueryParameter String value) {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      return Utils.validateValueOnEmpty(value, "CloudBees CD Url");
    }

    public FormValidation doCheckElectricFlowUser(@QueryParameter String value) {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      return Utils.validateValueOnEmpty(value, "CloudBees CD user");
    }

    public ListBoxModel doFillElectricFlowApiVersionItems() {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return new ListBoxModel();
      }

      ListBoxModel m = new ListBoxModel();

      m.add("Select api version", "");
      m.add("v1.0", "/rest/v1.0");

      return m;
    }

    public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item item) {
      return Credential.DescriptorImpl.doFillCredentialIdItems(item);
    }

    @RequirePOST
    public FormValidation doTestConnection(
            @QueryParameter("electricFlowUrl") final String electricFlowUrl,
            @QueryParameter("electricFlowUser") final String electricFlowUser,
            @QueryParameter("electricFlowPassword") final String electricFlowPassword,
            @QueryParameter("electricFlowApiVersion") final String electricFlowApiVersion,
            @QueryParameter("ignoreSslConnectionErrors") final boolean ignoreSslConnectionErrors,
            @QueryParameter @RelativePath("overrideCredential") String credentialId,
            @QueryParameter("credsType") String credsType,
            @AncestorInPath Item item)
        throws IOException {
      if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.ok();
      }

      if (electricFlowUrl.isEmpty()
          || electricFlowApiVersion.isEmpty()) {
        return FormValidation.error("Please fill required fields");
      }

      ElectricFlowClient efClient;
      if (credsType != null && credsType.equals("storedCreds")) {
        if (credentialId == null || credentialId.isEmpty()) {
          return FormValidation.error("Credentials are not provided");
        }
        StandardCredentials creds = new ItemCredentialHandler(item).getStandardCredentialsById(credentialId);
        efClient = ElectricFlowClientFactory.getElectricFlowClient(
                electricFlowUrl,
                creds,
                electricFlowApiVersion,
                ignoreSslConnectionErrors);
      } else {
        if (electricFlowUser == null || electricFlowUser.isEmpty()
            || electricFlowPassword == null || electricFlowPassword.isEmpty()) {
          return FormValidation.error("Credentials are not provided");
        }
        String decryptedPassword = Secret.fromString(electricFlowPassword).getPlainText();
        efClient =
                new ElectricFlowClient(
                        electricFlowUrl,
                        electricFlowUser,
                        decryptedPassword,
                        electricFlowApiVersion,
                        ignoreSslConnectionErrors);
      }

      try {
        efClient.testConnection();
        return FormValidation.ok("Success");
      } catch (Exception e) {
        log.warn("Wrong configuration - connection to CloudBees CD server failed", e);
        return FormValidation.error(
            "Wrong configuration - connection to CloudBees CD server failed. Error message: "
                + e.getMessage());
      }
    }

    @Override
    public String getDisplayName() {
      return "Configuration";
    }
  }
}
