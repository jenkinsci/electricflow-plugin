package com.electriccloud.plugin.spec.core.cibuilddetails

import com.electriccloud.plugin.spec.core.ResponseDecorator

class CiBuildDetail implements ResponseDecorator {

    final Map dslObject

    CiBuildDetail(Map dslObject) {
        this.dslObject = dslObject
    }

}
