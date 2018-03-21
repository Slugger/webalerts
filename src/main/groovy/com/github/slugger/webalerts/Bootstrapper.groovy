package com.github.slugger.webalerts

import com.github.slugger.webalerts.actions.CleanupAction
import com.github.slugger.webalerts.actions.ExecuteScriptAction
import com.github.slugger.webalerts.actions.NotificationProcessorAction
import com.github.slugger.webalerts.actions.TemplateProcessorAction
import com.github.slugger.webalerts.ctx.AppContext
import com.github.slugger.webalerts.ioc.AppModule
import com.github.slugger.webalerts.logging.Log4j2LogChute
import com.google.inject.Guice
import com.google.inject.Inject
import groovy.util.logging.Log4j2
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeConstants

@Log4j2
class Bootstrapper implements Runnable {

    static void main(String[] args) {
        def injector = Guice.createInjector(new AppModule())
        def bootstrapper = injector.getInstance(Bootstrapper)
        bootstrapper.args = args
        bootstrapper.run()
    }

    private final AppContext ctx
    private final ExecuteScriptAction executor
    private final TemplateProcessorAction tmplProcessor
    private final NotificationProcessorAction notifyProcessor
    private final CleanupAction cleanupAction

    private String[] args

    @Inject
    Bootstrapper(AppContext ctx, ExecuteScriptAction executor, TemplateProcessorAction tmplProcessor,
                 NotificationProcessorAction notifyProcessor, CleanupAction cleanupAction) {
        this.ctx = ctx
        this.executor = executor
        this.tmplProcessor = tmplProcessor
        this.notifyProcessor = notifyProcessor
        this.cleanupAction = cleanupAction
    }

    void run() {
        ctx.properties = loadProps()
        parseProperties()
        parseCommandLine()
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

    private void parseCommandLine() {
        def cli = new CliBuilder(usage: 'webalerts [options] <script> [script_options]')
        cli.x(longOpt: 'debug', 'Skip all post processing, dump script result to log')
        cli._(longOpt: 'skip-notification', 'Skip notification processing regardless of properties')
        cli._(longOpt: 'skip-template', 'Skip template processing regardless of properties')
        cli._(longOpt: 'cleanup', args: 1, argName: 'days', 'Remove files older than specified number of days from defined web root; ignores script execution')
        cli.h(longOpt: 'help', 'Print this usage help and exit immediately')
        def opts = cli.parse(args)
        if(opts == null) {
            System.exit(1)
        } else if(opts.'cleanup') {
            ctx.webRootAge = opts.'cleanup'.toInteger()
            cleanupAction.run()
            System.exit(0)
        } else if(opts.h || !opts.arguments()) {
            cli.usage()
            System.exit(1)
        }

        ctx.scriptName = opts.arguments()[0]
        ctx.scriptArgs = opts.arguments().tail()
        ctx.debugOnly = opts.x
        ctx.skipTemplateProcessing = opts.'skip-template'
        ctx.skipNotificationProcessing = opts.'skip-notification'
    }

    private Properties loadProps() {
        def props = new Properties()
        def propsFile = new File(new File(System.properties.'user.home'), '.webalerts.properties')
        propsFile.withReader('UTF-8') {
            props.load(it)
        }

        ['appRoot'].each {
            if(!props."$it")
                throw new RuntimeException("Missing $it in $propsFile")
        }

        def dir = new File(props.appRoot)
        if(!dir.isDirectory())
            throw new RuntimeException("Defined appRoot is invalid! [$dir]")
        def log4j = new File(dir, 'log4j2.xml')
        if(!log4j.canRead())
            throw new RuntimeException("$log4j not found!")

        props
    }

    private void parseProperties() {
        ctx.appRoot = new File(ctx.properties.appRoot)
        ctx.templateRoot = new File(ctx.appRoot, 'tmpls')
        ctx.scriptRoot = new File(ctx.appRoot, 'scripts')
        if(ctx.properties.webRoot)
            ctx.webRoot = new File(ctx.properties.webRoot)
    }

    private void initVelocity() {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, 'file')
        Velocity.setProperty('file.resource.loader.path', ctx.templateRoot.absolutePath)
        Velocity.setProperty(RuntimeConstants.INPUT_ENCODING, 'UTF-8')
        Velocity.setProperty(RuntimeConstants.OUTPUT_ENCODING, 'UTF-8')
        Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4j2LogChute.name)
        Velocity.setProperty('runtime.log.logsystem.log4j.logger', 'velocity')
        Velocity.init()
    }
}
