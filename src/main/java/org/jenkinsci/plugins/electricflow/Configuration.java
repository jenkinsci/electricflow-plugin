
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

    private final String credentialName;
    private final String electricFlowUser;
    private final String electricFlowPassword;
    private final String electricFlowUrl;
    private final String electricFlowApiVersion;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public Configuration(
            String credentialName,
            String electricFlowUrl,
            String electricFlowUser,
            String electricFlowPassword,
            String electricFlowApiVersion)
    {
        this.credentialName   = credentialName;
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

    public String getCredentialName()
    {
        return this.credentialName;
    }

    public String getElectricFlowApiVersion()
    {
        return this.electricFlowApiVersion;
    }

    public String getElectricFlowPassword()
    {
        return this.electricFlowPassword;
        // Secret encryptedPassword = Secret.fromString(this.electricFlowPassword);

        // return encryptedPassword.getPlainText();
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

    @Extension public static final class CredentialDescriptor
        extends Descriptor<Configuration>
    {

        //~ Methods ------------------------------------------------------------

        public FormValidation doCheckCredentialName(
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
                return FormValidation.error("Wrong credentials");
            }
        }

        @Override public String getDisplayName()
        {
            return "Configuration";
        }
    }
}
