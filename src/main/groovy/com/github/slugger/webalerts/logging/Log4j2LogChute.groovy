package com.github.slugger.webalerts.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.velocity.runtime.RuntimeServices
import org.apache.velocity.runtime.log.LogChute

class Log4j2LogChute implements LogChute {

    private Logger logger

    /**
     * Initializes this LogChute.
     * @param rs
     * @throws Exception
     */
    @Override
    void init(RuntimeServices rs) throws Exception {
        def category = rs.getProperty('runtime.log.logsystem.log4j.logger')
        if(!category)
            throw new RuntimeException('Velocity is misconfigured in the app code!')
        logger = LogManager.getLogger(category)
    }

    private String logChuteLevel2Log4jLevel(int level) {
        def name
        switch(level) {
            case LogChute.WARN_ID:
                name = 'warn'; break
            case LogChute.INFO_ID:
                name = 'info'; break
            case LogChute.TRACE_ID:
                name = 'trace'; break
            case LogChute.ERROR_ID:
                name = 'error'; break
            case LogChute.DEBUG_ID:
            default:
                name = 'debug'
        }
    }

    /**
     * Send a log message from Velocity.
     * @param level
     * @param message
     */
    @Override
    void log(int level, String message) {
        logger."${logChuteLevel2Log4jLevel(level)}"(message)
    }

    /**
     * Send a log message from Velocity along with an exception or error
     * @param level
     * @param message
     * @param t
     */
    @Override
    void log(int level, String message, Throwable t) {
        logger."${logChuteLevel2Log4jLevel(level)}"(message, t)
    }

    /**
     * Tell whether or not a log level is enabled.
     * @param level
     * @return True if a level is enabled.
     */
    @Override
    boolean isLevelEnabled(int level) {
        logger.isEnabled(Level.toLevel(logChuteLevel2Log4jLevel(level)))
    }
}
