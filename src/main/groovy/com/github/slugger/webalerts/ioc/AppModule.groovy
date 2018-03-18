package com.github.slugger.webalerts.ioc

import com.google.inject.AbstractModule

class AppModule extends AbstractModule {
    @Override
    void configure() {
        //bind(GroovyScriptEngine).toProvider(GroovyScriptEngineProvider).in(Singleton)
    }
}
