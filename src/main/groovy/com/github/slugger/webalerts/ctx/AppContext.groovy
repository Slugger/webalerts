package com.github.slugger.webalerts.ctx

import com.google.inject.Singleton
import groovy.json.JsonOutput

@Singleton
class AppContext {

    private Map state = [
        appRoot: [type: File, value: null],
        templateRoot: [type: File, value: null],
        webRoot: [type: File, value: null],
        scriptRoot: [type: File, value: null],
        scriptName: [type: String, value: null],
        scriptArgs: [type: List, value: null],
        debugOnly: [type: Boolean, value: null],
        skipTemplateProcessing: [type: Boolean, value: null],
        skipNotificationProcessing: [type: Boolean, value: null],
        properties: [type: Properties, value: null],
        scriptResult: [type: Object, value: null],
        skippedTemplate: [type: Boolean, value: null],
        skippedNotification: [type: Boolean, value: null],
        templateResult: [type: String, value: null]
    ]

    @Override
    def getProperty(String name) {
        def val = this.@state."$name"
        if(val == null)
            throw new RuntimeException("Invalid context property requested! [$name]")
        val.value
    }

    @Override
    void setProperty(String name, def value) {
        if(value == null)
            throw new NullPointerException("A context value cannot be set to null! [$name]")

        def prop = this.@state."$name"

        if(prop == null)
            throw new RuntimeException("Invalid context property! [$name]")
        if(prop.value != null)
            throw new RuntimeException("A context property can only be set once! [$name = $prop.value]")
        if(!(value in prop.type))
            throw new RuntimeException("Context property '$name' is defined as a $prop.type but attempted to assign value of type ${value.class}")

        prop.value = value
    }

    String debug() {
        state.toString()
    }
}
