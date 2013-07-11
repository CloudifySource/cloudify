package org.cloudifysource.shell.exceptions.handlers;

import java.util.logging.Level;

import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * @author noak
 * Exception handler for {@link org.cloudifysource.shell.exceptions.CLIValidationException}
 *
 * @since 2.7.0
 */
public class CLIValidationExceptionHandler extends AbstractClientSideExceptionHandler {

    private CLIValidationException e;

    public CLIValidationExceptionHandler(final CLIValidationException e) {
        this.e = e;
    }

    @Override
    public String getFormattedMessage() {
        String message = ShellUtils.getFormattedMessage(e.getReasonCode(), e.getArgs());
        if (message == null) {
            message = e.getReasonCode();
        }
        return message;
    }

    @Override
    public String getVerbose() {
       return e.getVerboseData();
    }

    @Override
    public Level getLoggingLevel() {
        return Level.WARNING;
    }
}
