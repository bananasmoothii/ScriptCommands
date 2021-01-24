package fr.bananasmoothii.scriptcommands.core;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomLogger {
    private final Logger logger;
    private boolean logThroughInfo;

    public CustomLogger(Logger logger) {
        this(logger, false);
    }

    public CustomLogger(Logger logger, boolean logThroughInfo) {
        this.logger = logger;
        this.logThroughInfo = logThroughInfo;
        if (logger.getLevel() == null)
            logger.setLevel(Level.INFO);
    }

    public void log(@NotNull Level level, String msg) {
        //System.out.println("                                                             logging at " + level + " (getLevel = " + logger.getLevel() + "): " + msg);
        if (!logThroughInfo || level.intValue() >= 800)
            logger.log(level, msg);
        else {
            if (level.intValue() == 700 && logger.getLevel().intValue() <= 700)
                logger.info("[CONFIG] "+msg);
            else if (level.intValue() == 500 && logger.getLevel().intValue() <= 500)
                logger.info("[FINE] "+msg);
            else if (level.intValue() == 400 && logger.getLevel().intValue() <= 400)
                logger.info("[FINER] "+msg);
            else if (level.intValue() == 300 && logger.getLevel().intValue() <= 300)
                logger.info("[FINEST] "+msg);
            else
                logger.info("[UNKNOWN IMPORTANCE] "+msg);
        }
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void warning(String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void config(String msg) {
        log(Level.CONFIG, msg);
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The string message (or a key in the message catalog)
     */
    public void finest(String msg) {
        log(Level.FINEST, msg);
    }

    public Level getLevel() {
        return logger.getLevel();
    }

    public void setLevel(@NotNull Level level) {
        System.out.println("level set: " + level);
        logger.setLevel(level);
    }

    public boolean isLogThroughInfo() {
        return logThroughInfo;
    }

    public void setLogThroughInfo(boolean logThroughInfo) {
        this.logThroughInfo = logThroughInfo;
    }
}
