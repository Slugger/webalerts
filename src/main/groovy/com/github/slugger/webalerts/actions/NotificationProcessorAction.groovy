package com.github.slugger.webalerts.actions

import com.github.slugger.webalerts.ctx.AppContext
import com.google.inject.Inject
import groovy.json.JsonOutput
import groovy.util.logging.Log4j
import org.simplejavamail.email.Email
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

@Log4j
class NotificationProcessorAction implements Action {

    private final AppContext ctx

    @Inject
    NotificationProcessorAction(AppContext ctx) {
        this.ctx = ctx
    }

    @Override
    void run() {
        if(skip())
            return
        sendEmail()
    }

    private boolean skip() {
        def skip = false
        if(ctx.debugOnly) {
            log.warn 'debugOnly = true; skipping notification processing'
            skip = true
        } else if(ctx.skipNotificationProcessing) {
            log.warn 'skipNotificationProcessing = true; skipping notification processing'
            skip = true
        } else if(Boolean.parseBoolean(ctx.properties.sendNotification?.trim())) {
            log.warn 'sendNotification = false; skipping notification processing'
            skip = true
        }
        skip
    }

    private void sendEmail() {
        def properties = ctx.properties
        def mailSettings = [from: properties.fromEmail,
                            to: properties.toEmail,
                            urlRoot: properties.urlRoot,
                            smtpHost: properties.smtpHost,
                            smtpPort: properties.smtpPort.toInteger(),
                            name: ctx.templateResult,
                            subject: ctx.scriptResult.subject ?: 'WebAlerts Result']
        log.debug "Sending email with following options:\n${JsonOutput.prettyPrint(JsonOutput.toJson(mailSettings))}"

        Email mail = EmailBuilder.startingBlank()
                .from(mailSettings.from)
                .to(mailSettings.to)
                .withSubject(mailSettings.subject)
                .withPlainText("$mailSettings.urlRoot/$mailSettings.name")
                .buildEmail()
        MailerBuilder.withSMTPServer(mailSettings.smtpHost, mailSettings.smtpPort)
                .buildMailer().sendMail(mail)
    }

}
