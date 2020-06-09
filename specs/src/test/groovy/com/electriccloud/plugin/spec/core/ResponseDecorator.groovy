package com.electriccloud.plugin.spec.core

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Inheriting from the class allows to skip defining fields and getters for the properties,
 * available in a DSL response.
 *
 * Do not
 */
trait ResponseDecorator {

    Logger log = Logger.getLogger(this.getClass().getSimpleName())

    void setLog(Logger log) {
        this.log = log
    }

    Logger getLog() {
        return this.log
    }

    abstract Map getDslObject()

    def propertyMissing(String name) {
        log.finer("Looking for a dsl property '$name' in ${this.getClass().getSimpleName()}")

        if (dslObject.containsKey(name))
            return (String) dslObject.get(name)

        log.warning("Failed to get property '$name' in ${this.getClass().getSimpleName()}")
        log.log(Level.FINER, dslObject.toMapString())

        return null
    }

    def methodMissing(String name, args) {
        // Intercept method that starts with get.
        if (name.startsWith("get")) {
            String propertyName = name.substring(3).uncapitalize()
            if (dslObject.containsKey(propertyName)) {
                def result = (String) dslObject[propertyName]
                // Add new method to class with metaClass.
                this.metaClass."$name" = { -> result }
                return result
            }
        }

        throw new MissingMethodException(name, this.class, args)
    }

//    def getProperty(String name) {
//        if (name == 'dslObject')
//            return dslObject
//
//        if (metaClass.hasProperty(name))
//            return metaClass.getProperty(this, name)
//
//        if (dslObject.containsKey(name))
//            return dslObject.get(name)
//
//        throw new NoSuchFieldException(
//                "Failed to get property '$name' from ${this.getClass().getSimpleName()}"
//        )
//
//    }
}
