package com.electriccloud.plugin.spec.nativeplugin.utils

interface JenkinsJob {

    boolean isSuccess()

    String getCiJobOutcome()

    boolean consoleLogContains(String logMessage)



}