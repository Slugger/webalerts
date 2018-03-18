package com.github.slugger.webalerts

import com.github.slugger.webalerts.ctx.AppContext
import com.github.slugger.webalerts.ioc.AppModule
import com.google.inject.Guice
import com.google.inject.Inject

// Isolated bootstrapper so we can set log4j props location
class Bootstrapper implements Runnable {

    static void main(String[] args) {
        def injector = Guice.createInjector(new AppModule())
        def bootstrapper = injector.getInstance(Bootstrapper)
        bootstrapper.args = args
        bootstrapper.run()
    }

    private final AppContext ctx
    private final Launcher launcher

    private String[] args

    @Inject
    Bootstrapper(AppContext ctx, Launcher launcher) {
        this.ctx = ctx
        this.launcher = launcher
    }

    @Override
    void run() {
        ctx.properties = loadProps()
        parseProperties()
        //System.properties.'log4j.configuration' = new File(ctx.appRoot, 'log4j.properties').toURI().toURL().toString()
        parseCommandLine()
        launcher.run()
    }

    private void parseProperties() {
        ctx.appRoot = new File(ctx.properties.appRoot)
        ctx.templateRoot = new File(ctx.appRoot, 'tmpls')
        ctx.scriptRoot = new File(ctx.appRoot, 'scripts')
        if(ctx.properties.webRoot)
            ctx.webRoot = new File(ctx.properties.webRoot)
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
        def log4j = new File(dir, 'log4j.properties')
        if(!log4j.canRead())
            throw new RuntimeException("$log4j not found!")

        props
    }

    private void parseCommandLine() {
        def cli = new CliBuilder(usage: 'webalerts [options] <script> [script_options]')
        cli.x(longOpt: 'debug', 'Skip all post processing, dump script result to log')
        cli._(longOpt: 'skip-notification', 'Skip notification processing regardless of properties')
        cli._(longOpt: 'skip-template', 'Skip template processing regardless of properties')
        cli.h(longOpt: 'help', 'Print this usage help and exit immediately')
        def opts = cli.parse(args)
        if(opts.h || !opts.arguments()) {
            cli.usage()
            System.exit(1)
        }

        ctx.scriptName = opts.arguments()[0]
        ctx.scriptArgs = opts.arguments().tail()
        ctx.debugOnly = opts.x
        ctx.skipTemplateProcessing = opts.'skip-template'
        ctx.skipNotificationProcessing = opts.'skip-notification'
    }

}
