
// ElectricFlowConfigurationManager.java --
//
// ElectricFlowConfigurationManager.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.util.List;

public class ElectricFlowConfigurationManager
{

    //~ Instance fields --------------------------------------------------------

    public List<Configuration> efConfigurations;

    //~ Constructors -----------------------------------------------------------

    public ElectricFlowConfigurationManager()
    {
        efConfigurations = Utils.getCredentials();
    }

    //~ Methods ----------------------------------------------------------------

    public Configuration getCredentialByName(String name)
    {

        for (Configuration cred : this.efConfigurations) {

            if (cred.getCredentialName()
                    .equals(name)) {
                return cred;
            }
        }

        return null;
    }
}
