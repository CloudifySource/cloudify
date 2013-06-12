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

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 8:03 PM
 * <br/><br/>
 *
 * Interface for handling exception in the CLI.
 *
 * @since 2.6.0
 */
public interface ClientSideExceptionHandler {

    /**
     *
     * @param verbose Verbose mode or not.
     * @return Final message to be logged in the CLI.
     */
    String getMessage(boolean verbose);

    /**
     * Each exception corresponds to a logging level.
     * Expected exception may be a WARNING level log.
     * though unexpected throwables are always SEVERE.
     * @return The logging level of this specific handler.
     */
    Level getLoggingLevel();
}
