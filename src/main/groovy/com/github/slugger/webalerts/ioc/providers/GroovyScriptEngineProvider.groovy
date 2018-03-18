package com.github.slugger.webalerts.ioc.providers

import com.github.slugger.webalerts.ctx.AppContext
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import groovy.io.FileType
import groovy.util.logging.Log4j2

@Log4j2
@Singleton
class GroovyScriptEngineProvider implements Provider<GroovyScriptEngine> {

    private final AppContext ctx

    @Inject
    GroovyScriptEngineProvider(AppContext ctx) {
        this.ctx = ctx
    }

    @Override
    GroovyScriptEngine get() {
        new GroovyScriptEngine(createClasspathUrls())
    }

    private URL[] createClasspathUrls() {
        def urls = [ctx.scriptRoot.toURI().toURL()]
        def libRoot = new File(ctx.appRoot, 'lib')
        if(libRoot.isDirectory())
            libRoot.eachFile(FileType.FILES) {
                if(it.name.endsWith('.jar'))
                    urls << it.toURI().toURL()
            }
        log.debug "ScriptEngine classpath: $urls"
        urls
    }
}
