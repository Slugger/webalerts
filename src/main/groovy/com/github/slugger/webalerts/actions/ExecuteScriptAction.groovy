package com.github.slugger.webalerts.actions

import com.github.slugger.webalerts.ctx.AppContext
import com.github.slugger.webalerts.ioc.providers.GroovyScriptEngineProvider
import com.google.inject.Inject
import groovy.util.logging.Log4j
import org.apache.log4j.Logger

@Log4j
class ExecuteScriptAction implements Action {

    private final AppContext ctx
    private final GroovyScriptEngineProvider engineProvider

    @Inject
    ExecuteScriptAction(AppContext ctx, GroovyScriptEngineProvider engineProvider) {
        this.ctx = ctx
        this.engineProvider = engineProvider
    }

    @Override
    void run() {
        log.debug "Executing ${new File(ctx.scriptRoot, ctx.scriptName).absolutePath}"
        ctx.scriptResult = engineProvider.get().run(ctx.scriptName, createBinding())
    }

    private Binding createBinding() {
        def binding = new Binding()
        binding.setVariable('props', ctx.properties)
        binding.setVariable('log', Logger.getLogger('*SCRIPT*'))
        binding.setVariable('args', ctx.scriptArgs)
        if(log.isTraceEnabled()) {
            binding.variables.each { k, v ->
                log.trace "Binding var '$k' = $v"
            }
        }
        binding
    }
}
