package com.github.slugger.webalerts

import com.github.slugger.webalerts.actions.ExecuteScriptAction
import com.github.slugger.webalerts.actions.NotificationProcessorAction
import com.github.slugger.webalerts.actions.TemplateProcessorAction
import com.github.slugger.webalerts.ctx.AppContext
import com.google.inject.Inject
import groovy.util.logging.Log4j
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.log.Log4JLogChute

@Log4j
class Launcher implements Runnable {

    private final AppContext ctx
    private final ExecuteScriptAction executor
    private final TemplateProcessorAction tmplProcessor
    private final NotificationProcessorAction notifyProcessor

    @Inject
    Launcher(AppContext ctx, ExecuteScriptAction executor, TemplateProcessorAction tmplProcessor, NotificationProcessorAction notifyProcessor) {
        this.ctx = ctx
        this.executor = executor
        this.tmplProcessor = tmplProcessor
        this.notifyProcessor = notifyProcessor
    }

    void run() {
        initVelocity()
        log.debug "Initial context:\n${ctx.debug()}"
        try {
            executor.run()
            tmplProcessor.run()
            notifyProcessor.run()
        } catch(Throwable t) {
            log.fatal 'Unexpected error', t
            System.exit(1)
        }
    }

    // Have to do this here to prevent log4j from initing itself too early
    private void initVelocity() {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, 'file')
        Velocity.setProperty('file.resource.loader.path', ctx.templateRoot.absolutePath)
        Velocity.setProperty(RuntimeConstants.INPUT_ENCODING, 'UTF-8')
        Velocity.setProperty(RuntimeConstants.OUTPUT_ENCODING, 'UTF-8')
        Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.name)
        Velocity.setProperty('runtime.log.logsystem.log4j.logger', 'velocity')
        Velocity.init()
    }

}
