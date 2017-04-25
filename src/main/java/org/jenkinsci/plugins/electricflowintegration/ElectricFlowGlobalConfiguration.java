
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
        List<Configuration> configurations = req.bindJSONToList(
                Configuration.class, formData.get("configurations"));

        this.efConfigurations = configurations;
        save();

        return true;
    }

    public List<Configuration> getConfigurations()
    {
        return this.efConfigurations;
    }
}
