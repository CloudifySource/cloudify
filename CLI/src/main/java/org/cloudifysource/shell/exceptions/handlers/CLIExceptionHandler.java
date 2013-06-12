/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.exceptions.handlers;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.shell.exceptions.CLIException;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 7:47 PM
 * <br/><br/>
 *
 * Exception handler for {@link CLIException}
 *
 * @since 2.6.0
 *
 */
public class CLIExceptionHandler extends AbstractClientSideExceptionHandler {

    private CLIException e;

    public CLIExceptionHandler(final CLIException e) {
        this.e = e;
    }

    @Override
    public String getFormattedMessage() {
        return e.getMessage();
    }

    @Override
    public String getVerbose() {
        if (e.getVerbose() == null) {
            return ExceptionUtils.getFullStackTrace(e);
        } else {
            return e.getVerbose();
        }
    }

    @Override
    public Level getLoggingLevel() {
        return Level.WARNING;
    }
}
