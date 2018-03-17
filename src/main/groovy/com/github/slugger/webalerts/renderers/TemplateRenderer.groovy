package com.github.slugger.webalerts.renderers

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.tools.generic.EscapeTool

class TemplateRenderer {

    private final File tmplRoot

    TemplateRenderer(File tmplRoot) {
        this.tmplRoot = tmplRoot
    }

    String render(Map result) {
        def output = new ByteArrayOutputStream()
        def ctx = new VelocityContext()
        ctx.put('result', result)
        ctx.put('esc', new EscapeTool())
        def tmpl = Velocity.getTemplate(result.tmpl)
        new OutputStreamWriter(output, 'UTF-8').withWriter {
            tmpl.merge(ctx, it)
        }
        output.toString('UTF-8')
    }
}
