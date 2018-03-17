package com.github.slugger.webalerts

import com.github.slugger.webalerts.renderers.TemplateRenderer
import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.util.logging.Log4j
import org.apache.log4j.Logger
import org.apache.velocity.app.Velocity
import org.apache.velocity.runtime.RuntimeConstants
import org.simplejavamail.email.Email
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

@Log4j
class Launcher implements Runnable {

    private final String[] args
    private final Properties properties
    private final GroovyScriptEngine engine
    private final Binding binding
    private final File scriptRoot
    private final String scriptName
    private final File appRoot

    Launcher(Properties props, String... args) {
        this.args = args
        properties = props
        checkProps()
        appRoot = new File(properties.appRoot)
        scriptRoot = new File(appRoot, 'scripts')
        binding = new Binding()
        engine = new GroovyScriptEngine(getScriptClasspathUrls())
        scriptName = args.size() ? args[0] : 'script.groovy'
        initVelocity()
    }

    void run() {
        try {
            doBinding()
            log.debug "Executing '${new File(scriptRoot, scriptName).absolutePath}'"
            def result = engine.run(scriptName, binding)
            if (result instanceof Map) {
                def tmplResult
                if (result.tmpl && result.size) {
                    tmplResult = new TemplateRenderer(new File(appRoot, 'tmpls')).render(result)
                    def saveHtml = Boolean.parseBoolean(properties.saveHtml?.trim())
                    if (saveHtml && properties.webRoot) {
                        def webRoot = new File(properties.webRoot)
                        if (!webRoot.exists())
                            webRoot.mkdirs()
                        def out = File.createTempFile('webalerts_', '.html', webRoot)
                        out << tmplResult
                        log.debug "Wrote template merge result to $out"
                        def doEmail = Boolean.parseBoolean(properties.sendEmail?.trim())
                        if (doEmail) {
                            sendEmail(out.name, result.subject ?: 'Results from tvalerts')
                        } else
                            log.info 'sendEmail is disabled in app properties; skipping notification processing'
                    } else if (!saveHtml)
                        log.info 'saveHtml is disabled in app properties; skipping merge of template'
                    else
                        log.info 'No webRoot defined in app properties; skipping write of template to disk'
                } else if(!result.tmpl)
                    log.info 'No tmpl defined in script result; skipping template processing'
                else
                    log.info 'Result size reported as 0; skipping template processing'
            } else
                log.warn "Script result is not a map; skipping all post processing!"
        } catch(Throwable t) {
            log.fatal 'Unexpected error', t
            System.exit(1)
        }
    }

    private void sendEmail(String name, String subject) {
        def mailSettings = [from: properties.fromEmail,
            to: properties.toEmail,
            urlRoot: properties.urlRoot,
            smtpHost: properties.smtpHost,
            smtpPort: properties.smtpPort.toInteger()]
        log.debug "Sending email with following options:\n${JsonOutput.prettyPrint(JsonOutput.toJson(mailSettings))}\nname: $name\nsubject: $subject"

        Email mail = EmailBuilder.startingBlank()
                .from(mailSettings.from)
                .to(mailSettings.to)
                .withSubject(subject)
                .withPlainText("$mailSettings.urlRoot/$name")
                .buildEmail()
        MailerBuilder.withSMTPServer(mailSettings.smtpHost, mailSettings.smtpPort)
                .buildMailer().sendMail(mail)
    }

    private URL[] getScriptClasspathUrls() {
        def urls = [scriptRoot.toURI().toURL()]
        def libRoot = new File(appRoot, 'lib')
        if(libRoot.isDirectory())
            libRoot.eachFile(FileType.FILES) {
                if(it.name.endsWith('.jar'))
                    urls << it.toURI().toURL()
            }
        log.debug "JAR URLs: $urls"
        urls
    }

    private void doBinding() {
        binding.setVariable('props', properties)
        binding.setVariable('log', Logger.getLogger('*SCRIPT*'))
    }

    private void initVelocity() {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, 'file')
        Velocity.setProperty('file.resource.loader.path', new File(appRoot, 'tmpls').absolutePath)
        Velocity.setProperty(RuntimeConstants.INPUT_ENCODING, 'UTF-8')
        Velocity.setProperty(RuntimeConstants.OUTPUT_ENCODING, 'UTF-8')
        Velocity.setProperty('runtime.log.logsystem.log4j.logger', 'velocity')
        Velocity.init()
    }

    private void checkProps() {
    }
}
