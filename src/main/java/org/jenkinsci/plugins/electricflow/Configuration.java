
// Configuration.java --
//
// Configuration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.IOException;

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

    //~ Instance fields --------------------------------------------------------

    private final String configurationName;
    private final String electricFlowUser;
    private final String electricFlowPassword;
    private final String electricFlowUrl;
    private final String electricFlowApiVersion;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public Configuration(
            String configurationName,
            String electricFlowUrl,
            String electricFlowUser,
            String electricFlowPassword,
            String electricFlowApiVersion)
    {
        this.configurationName   = configurationName;
        this.electricFlowUrl  = electricFlowUrl;
        this.electricFlowUser = electricFlowUser;
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
            m.add("v1", "v1");

            return m;
        }

        @Override
        public String getHelpFile() {
            return super.getHelpFile();
        }



        public FormValidation doTestConnection(
                @QueryParameter("electricFlowUrl") final String electricFlowUrl,
                @QueryParameter("electricFlowUser") final String electricFlowUser,
                @QueryParameter("electricFlowPassword") final String electricFlowPassword)
            throws IOException
        {

            try {
                Secret encryptedPassword = Secret.fromString(electricFlowPassword);
                String decryptedPassword = encryptedPassword.getPlainText();
                ElectricFlowClient efClient = new ElectricFlowClient(
                        electricFlowUrl, electricFlowUser,
                        decryptedPassword);

                efClient.getSessionId();

                return FormValidation.ok("Success");
            }
            catch (Exception e) {
                return FormValidation.error("Wrong configurations");
            }
        }

        @Override public String getDisplayName()
        {
            return "Configuration";
        }
    }
}
