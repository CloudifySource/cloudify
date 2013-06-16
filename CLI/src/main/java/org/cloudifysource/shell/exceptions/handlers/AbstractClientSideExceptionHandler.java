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

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:37 PM
 * <br/><br/>
 *
 * Abstract class for exception handlers in the CLI.
 * This class appends the formatted message with verbose data if necessary.
 *
 * @since 2.6.0
 *
 */
public abstract class AbstractClientSideExceptionHandler implements ClientSideExceptionHandler {

    /**
     *
     * @return The formatted message to be displayed on the console.
     */
    public abstract String getFormattedMessage();

    /**
     *
     * @return Verbose information - Stack trace.
     */
    public abstract String getVerbose();

    @Override
    public String getMessage(final boolean verbose) {

        if (verbose) {
            // display the stack trace if present
            final String stackTrace = getVerbose();
            if (stackTrace != null) {
                return getFormattedMessage() + " : " + stackTrace;
            }
			return getFormattedMessage();
        }
		return getFormattedMessage();
    }
}
