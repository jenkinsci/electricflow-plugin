package com.electriccloud.plugin.spec.core.application

import com.electriccloud.plugin.spec.core.Job
import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.spec.PluginSpockTestSupport

class Process implements ResponseDecorator {

    String projectName
    String name
    Application application

    @Lazy
    ArrayList<ProcessRun> runs = { _retrieveProcessRuns() }()

    @Lazy
    Map dslObject = { _retrieveProcessDetails() }()

    Process(String projectName, String applicationName, String processName) {
        Process(new Application(projectName, applicationName), processName)
    }

    Process(Application application, String processName) {
        this.application = application
        this.projectName = application.getProjectName()
        this.name = processName
    }

    Map _retrieveProcessDetails() {
        def response = (new PluginSpockTestSupport()).dsl("""
            getProcess(
              projectName    : '${projectName}',
              applicationName: '${application.getName()}',
              processName    : '${name}'
            )
        """)

        return response['process'] as Map
    }

    ArrayList<ProcessRun> _retrieveProcessRuns() {
        ArrayList<Job> result = Job.findJobs(
                // Filter
                [
                        [
                                propertyName: 'applicationName',
                                operator    : 'isNotNull'
                        ], [
                                propertyName: 'applicationName',
                                operator    : 'equals',
                                operand1    : application.getName()
                        ], [
                                propertyName: 'projectName',
                                operator    : 'equals',
                                operand1    : projectName
                        ], [
                                propertyName: 'jobName',
                                operator    : 'contains',
                                operand1    : name
                        ]
                ],
                // Sort
                [[propertyName: "createTime", order: "ascending"]]
        )
        return result as ArrayList<ProcessRun>
    }
}
