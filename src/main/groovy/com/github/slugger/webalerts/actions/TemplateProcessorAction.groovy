package com.github.slugger.webalerts.actions

import com.github.slugger.webalerts.ctx.AppContext
import com.github.slugger.webalerts.renderers.TemplateRenderer
import com.google.inject.Inject
import groovy.util.logging.Log4j2

@Log4j2
class TemplateProcessorAction implements Action {

    private final AppContext ctx
    private final TemplateRenderer tmplRenderer

    @Inject
    TemplateProcessorAction(AppContext ctx, TemplateRenderer tmplRenderer) {
        this.ctx = ctx
        this.tmplRenderer = tmplRenderer
    }

    @Override
    void run() {
        if(skip() || invalidResult()) {
            ctx.skippedTemplate = true
            return
        }

        String tmplResult = tmplRenderer.render()
        def saveHtml = Boolean.parseBoolean(ctx.properties.saveHtml?.trim())
        if (saveHtml && ctx.webRoot) {
            if (!ctx.webRoot.exists())
                ctx.webRoot.mkdirs()
            def out = File.createTempFile('webalerts_', '.html', ctx.webRoot)
            out << tmplResult
            log.debug "Wrote template merge result to $out"
            ctx.templateResult = out.name
        } else if (!saveHtml) {
            log.warn 'saveHtml is disabled in app properties; skipping merge of template'
            log.warn "Template result:\n$tmplResult"
        } else {
            log.warn 'No webRoot defined in app properties; skipping write of template to disk'
            log.warn "Template result:\n$tmplResult"
        }
    }

    private boolean invalidResult() {
        def invalid = false
        def result = ctx.scriptResult
        if(result == null) {
            log.warn "Script result is null, can't process!"
            invalid = true
        } else if(!(result in Map)) {
            log.warn "Script result is not a map, can't process! [${result.class}]"
            invalid = true
        } else if(result.size < 1) {
            log.warn "Script result reports size < 1, skipping template processing [size = ${result.size}]"
            invalid = true
        } else if(!result.tmpl) {
            log.warn "Script result does not specify a template, can't process! [tmpl = $result.tmpl]"
            invalid = true
        }
        invalid
    }

    private boolean skip() {
        def skip = false
        if(ctx.debugOnly) {
            log.warn 'debugOnly = true; skipping template processing'
            log.warn "SCRIPT RESULT:\n$ctx.scriptResult"
            skip = true
        } else if(ctx.skipTemplateProcessing) {
            log.warn 'skipTemplateProcessing = true; skipping template processing'
            skip = true
        }
        skip
    }
}
