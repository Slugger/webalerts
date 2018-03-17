package com.github.slugger.webalerts

// Isolated bootstrapper so we can set log4j props location
class Bootstrapper {

    static private Properties props

    static void main(String[] args) {
        props = loadProps()
        configLogging()
        new Launcher(props, args).run()
    }

    static private void configLogging() {
        System.properties.'log4j.configuration' = new File(new File(props.appRoot), 'log4j.properties').toURI().toURL().toString()
    }

    static private Properties loadProps() {
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
}
