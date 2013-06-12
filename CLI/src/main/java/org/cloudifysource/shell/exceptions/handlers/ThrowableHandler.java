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

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:51 PM
 *
 * Exception handler for {@link Throwable}
 *
 * @since 2.6.0
 *
 */
public class ThrowableHandler extends AbstractClientSideExceptionHandler {

    private Throwable t;

    public ThrowableHandler(final Throwable t) {
        this.t = t;
    }

    @Override
    public String getFormattedMessage() {
        return t.getMessage();
    }

    @Override
    public String getVerbose() {
        return ExceptionUtils.getFullStackTrace(t);
    }

    @Override
    public Level getLoggingLevel() {
        return Level.SEVERE;
    }
}
