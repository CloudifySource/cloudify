/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
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
