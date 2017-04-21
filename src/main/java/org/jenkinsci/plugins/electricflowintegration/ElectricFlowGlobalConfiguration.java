
// ElectricFlowGlobalConfiguration.java --
//
// ElectricFlowGlobalConfiguration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.util.List;

import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;

import jenkins.model.GlobalConfiguration;

@Extension public class ElectricFlowGlobalConfiguration
    extends GlobalConfiguration
{

    //~ Instance fields --------------------------------------------------------

    public List<Configuration> efConfigurations;

    //~ Constructors -----------------------------------------------------------

    public ElectricFlowGlobalConfiguration()
    {
        load();
    }

    //~ Methods ----------------------------------------------------------------

    @Override public boolean configure(
            StaplerRequest req,
            JSONObject     formData)
        throws FormException
    {
//        System.out.println("Config object: " + formData.toString());

        List<Configuration> configurations = req.bindJSONToList(
                Configuration.class, formData.get("configurations"));

//        System.out.println("Credentials after map: "
//                + configurations.toString());

//        for (Configuration cred : configurations) {
//            System.out.println("===");
//            System.out.println("Cred name: " + cred.getCredentialName());
//            System.out.println("URL: " + cred.getElectricFlowUrl());
//            System.out.println("Name: " + cred.getElectricFlowUser());
//            System.out.println("Password: " + cred.getElectricFlowPassword());
//            System.out.println("===");
//        }

        this.efConfigurations = configurations;
        save();

        return true;
    }

    public List<Configuration> getConfigurations()
    {
        return this.efConfigurations;
    }
}
