package com.electriccloud.plugin.spec.core.cibuilddetails

import com.electriccloud.plugin.spec.core.ResponseDecorator
import com.electriccloud.plugin.spec.core.pipeline.PipelineRun
import com.electriccloud.plugin.spec.core.release.Release
import com.electriccloud.spec.PluginSpockTestSupport
import net.sf.json.JSONObject

/**
 * CBuildDetails are attached to the FlowRuntime or Release inside of a container CiBuildDetailInfo
 * This container holds some properties for the CiBuildData
 */
class CiBuildDetailInfo implements ResponseDecorator {

    private final String name
    private final String projectName

    public Release attachedRelease
    public PipelineRun attachedFlowRuntime

    @Lazy
    HashMap<String, Object> dslObject = {
        _retrieveCiBuildDetailInfo()
    }()

    @Lazy
    CiBuildDetail ciBuildDetail = {
        new CiBuildDetail(dslObject['ciBuildDetail'] as Map)
    }()

    @Lazy
    JSONObject buildData = { JSONObject.fromObject(getDslObject()['buildData']) }()

    @Lazy
    TestResults testResults = { new TestResults((Map) dslObject['testResults']) }()

    @Lazy
    List<ArtifactDetails> artifacts = {
        dslObject['artifacts']['artifact'].collect({
            return new ArtifactDetails((Map) it)
        })
    }()

    /**
     * Creates a release attached CIBuildDetailInfo object
     * @param name
     * @param release
     */
    CiBuildDetailInfo(String name, Release release) {
        this.name = name
        this.attachedRelease = release
        this.projectName = release.getProjectName()
    }

    CiBuildDetailInfo(String name, PipelineRun flowRuntime) {
        this.name = name
        this.attachedFlowRuntime = flowRuntime
        this.projectName = flowRuntime.getProjectName()
    }

    String getName() {
        return name
    }

    String getProjectName() {
        return projectName
    }

    Release getAttachedRelease() {
        return attachedRelease
    }

    PipelineRun getAttachedFlowRuntime() {
        return attachedFlowRuntime
    }

    private HashMap<String, Object> _retrieveCiBuildDetailInfo() {
        def attachedDsl
        if (this.attachedFlowRuntime != null) {
            attachedDsl = "flowRuntimeId: '${attachedFlowRuntime.getId()}'"
        } else {
            attachedDsl = "releaseName: '${this.attachedRelease.getName()}'"
        }

        def result = (new PluginSpockTestSupport()).dsl("""
            getCIBuildDetail(ciBuildDetailName: '$name', projectName: '$projectName', $attachedDsl )
        """)

        return result['ciBuildDetailInfo'] as HashMap<String, Object>
    }

}
