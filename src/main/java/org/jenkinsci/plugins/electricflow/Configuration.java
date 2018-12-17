
// Configuration.java --
//
// Configuration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

/**
 * Configuration to access ElectricFlow server.
 */
public class Configuration
    extends AbstractDescribableImpl<Configuration>
{

    private static final Log log = LogFactory.getLog(Configuration.class);

    private final String configurationName;
    private final String electricFlowUser;
    private final String electricFlowPassword;
    private final String electricFlowUrl;
    private final String electricFlowApiVersion;
    private final boolean ignoreSslConnectionErrors;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public Configuration(
            String configurationName,
            String electricFlowUrl,
            String electricFlowUser,
            String electricFlowPassword,
            String electricFlowApiVersion,
            boolean ignoreSslConnectionErrors)
    {
        this.configurationName = configurationName;
        this.electricFlowUrl   = electricFlowUrl;
        this.electricFlowUser  = electricFlowUser;

        if (!electricFlowPassword.equals(this.getElectricFlowPassword())) {

            // encrypted one
            Secret secret = Secret.fromString(electricFlowPassword);

            this.electricFlowPassword = secret.getEncryptedValue();
        }
        else {
            this.electricFlowPassword = electricFlowPassword;
        }

        // end
        this.electricFlowApiVersion = electricFlowApiVersion;

        this.ignoreSslConnectionErrors = ignoreSslConnectionErrors;
    }

    //~ Methods ----------------------------------------------------------------

    public String getConfigurationName()
    {
        return this.configurationName;
    }

    public String getElectricFlowApiVersion()
    {
        return this.electricFlowApiVersion;
    }

    public boolean getIgnoreSslConnectionErrors()
    {
        return this.ignoreSslConnectionErrors;
    }

    public String getElectricFlowPassword()
    {
        return this.electricFlowPassword;
    }

    public String getElectricFlowUrl()
    {
        return this.electricFlowUrl;
    }

    public String getElectricFlowUser()
    {
        return this.electricFlowUser;
    }

    //~ Inner Classes ----------------------------------------------------------

    @Extension public static final class ConfigurationDescriptor
        extends Descriptor<Configuration>
    {

        //~ Methods ------------------------------------------------------------

        public FormValidation doCheckConfigurationName(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Configuration name");
        }

        public FormValidation doCheckElectricFlowApiVersion(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value,
                "ElectricFlow api version");
        }

        public FormValidation doCheckElectricFlowPassword(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "ElectricFlow password");
        }

        public FormValidation doCheckElectricFlowUrl(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "ElectricFlow Url");
        }

        public FormValidation doCheckElectricFlowUser(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "ElectricFlow user");
        }

        public ListBoxModel doFillElectricFlowApiVersionItems()
        {
            ListBoxModel m = new ListBoxModel();

            m.add("Select api version", "");
            m.add("v1.0", "/rest/v1.0");

            return m;
        }

        public FormValidation doTestConnection(
                @QueryParameter("electricFlowUrl") final String electricFlowUrl,
                @QueryParameter("electricFlowUser") final String electricFlowUser,
                @QueryParameter("electricFlowPassword") final String electricFlowPassword,
                @QueryParameter("electricFlowApiVersion") final String electricFlowApiVersion,
                @QueryParameter("ignoreSslConnectionErrors") final boolean ignoreSslConnectionErrors)
            throws IOException
        {

            if (electricFlowUrl.isEmpty() || electricFlowUser.isEmpty()
                    || electricFlowPassword.isEmpty() || electricFlowApiVersion.isEmpty()) {
                return FormValidation.error("Please fill required fields");
            }

            try {
                String             decryptedPassword = Secret.fromString(
                                                                 electricFlowPassword)
                                                             .getPlainText();
                ElectricFlowClient efClient          = new ElectricFlowClient(
                        electricFlowUrl, electricFlowUser, decryptedPassword,
                        electricFlowApiVersion, ignoreSslConnectionErrors);

                efClient.testConnection();

                return FormValidation.ok("Success");
            }
            catch (Exception e) {
                log.warn("Wrong configuration - connection to Electric Flow server failed", e);
                return FormValidation.error("Wrong configuration - connection to Electric Flow server failed. Error message: " + e.getMessage());
            }
        }

        @Override public String getDisplayName()
        {
            return "Configuration";
        }
    }
}
