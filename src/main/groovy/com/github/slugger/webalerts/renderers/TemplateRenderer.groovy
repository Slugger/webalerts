package com.github.slugger.webalerts.renderers

import com.github.slugger.webalerts.ctx.AppContext
import com.google.inject.Inject
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.tools.generic.EscapeTool

class TemplateRenderer {

    private final AppContext ctx

    @Inject
    TemplateRenderer(AppContext ctx) {
        this.ctx = ctx
    }

    String render() {
        def output = new ByteArrayOutputStream()
        def tmplCtx = new VelocityContext()
        tmplCtx.put('result', ctx.scriptResult)
        tmplCtx.put('esc', new EscapeTool())
        def tmpl = Velocity.getTemplate(ctx.scriptResult.tmpl)
        new OutputStreamWriter(output, 'UTF-8').withWriter {
            tmpl.merge(tmplCtx, it)
        }
        output.toString('UTF-8')
    }
}
