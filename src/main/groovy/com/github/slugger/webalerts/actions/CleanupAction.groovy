package com.github.slugger.webalerts.actions

import com.github.slugger.webalerts.ctx.AppContext
import com.google.inject.Inject
import groovy.util.logging.Log4j2

@Log4j2
class CleanupAction implements Action {
    static private final long ONE_DAY_MS = 86400000L

    static private boolean isFileOlderThanNumberOfDays(File f, int days) {
        (new Date().time - (ONE_DAY_MS * days)) > f.lastModified()
    }

    private final AppContext ctx

    @Inject
    CleanupAction(AppContext ctx) {
        this.ctx = ctx
    }

    @Override
    void run() {
        if (!ctx.webRoot) {
            log.warn 'There is no web root defined in the app properties so there is nothing to do!'
            return
        }

        log.debug "Checking $ctx.webRoot for files more than $ctx.webRootAge day(s) old"
        ctx.webRoot.eachFile {
            if(isFileOlderThanNumberOfDays(it, ctx.webRootAge)) {
                if(it.delete())
                    log.debug "Deleted $it"
                else
                    log.error "Failed to delete $it"
            }
        }
    }
}
