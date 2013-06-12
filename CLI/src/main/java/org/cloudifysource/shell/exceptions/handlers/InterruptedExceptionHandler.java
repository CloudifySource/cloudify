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

import org.cloudifysource.shell.ShellUtils;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:54 PM
 *
 * Exception handler for {@link org.cloudifysource.shell.exceptions.CLIStatusException}
 *
 * @since 2.6.0
 *
 */
public class InterruptedExceptionHandler extends AbstractClientSideExceptionHandler {

    @Override
    public String getFormattedMessage() {
        return ShellUtils.getFormattedMessage("command_interrupted", new Object[] {});
    }

    @Override
    public String getVerbose() {
        return null;
    }

    @Override
    public Level getLoggingLevel() {
        return Level.SEVERE;
    }
}
