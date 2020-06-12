package com.electriccloud.plugin.spec.core.cibuilddetails

import com.electriccloud.plugin.spec.core.ResponseDecorator

class TestResults implements ResponseDecorator {

    final Map<String, Object> dslObject

    TestResults(Map dslObject) {
        this.dslObject = dslObject
    }

    int getDuration() {
        return Integer.parseInt(dslObject['duration'] as String)
    }

    int getPassPercentage() {
        return Integer.parseInt(dslObject['passPercentage'] as String)

    }

    int getTotalCount() {
        return Integer.parseInt(dslObject['totalCount'] as String)
    }

    int getFailPercentage() {
        return Integer.parseInt(dslObject['failPercentage'] as String)
    }

}
