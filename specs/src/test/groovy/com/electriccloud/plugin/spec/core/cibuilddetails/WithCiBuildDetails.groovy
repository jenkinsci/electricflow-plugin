package com.electriccloud.plugin.spec.core.cibuilddetails

import com.electriccloud.plugin.spec.core.ResponseDecorator

trait WithCiBuildDetails extends ResponseDecorator {

    abstract ArrayList<CiBuildDetailInfo> getCiBuildDetails()

    CiBuildDetailInfo findCiBuildDetailInfo(String name) {
        for (CiBuildDetailInfo cbdi : getCiBuildDetails()) {
            def cbdiName = cbdi.getName()
            if (cbdiName.equals(name)) {
                return cbdi
            }
        }

        return null
    }

}