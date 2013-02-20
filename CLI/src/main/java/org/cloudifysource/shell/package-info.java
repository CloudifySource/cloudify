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
 ******************************************************************************/

/**************************
 * @author rafip, noak
 * @since 2.0.0
 * <p>
 * 
 * {@link org.cloudifysource.shell.GigaShellMain} - Extends the karaf framework Main object.
 * <br>
 * This class is used to start the shell/console.
 * <p>
 * 
 * {@link org.cloudifysource.shell.ConsoleWithProps} - Extends the karaf framework Console Object.
 * <br>
 * This class and its extending classes adds some branding and functionality on top of the base class,
 * e.g. adds a default adminFacade to the session, sets the prompt text, sets a default application and more.
 * <p>
 * 
 * {@link org.cloudifysource.shell.ConditionLatch} - waits for a specific process (defined as a
 * {@link com.gigaspaces.internal.utils.ConditionLatch.Predicate})
 * to complete.
 * <br>
 * It samples its status according to a specified polling interval and if the process is not completed
 * before the specified timeout is reached, a {@link java.util.concurrent.TimeoutException} is thrown, with the 
 * configured error message.
 * <p>
 * 
 * {@link org.cloudifysource.shell.AdminFacade} - The Interface/API of the admin facade. 
 * The Admin facade is used by commands when Admin API logic is required.
 *  
 *****************************/

package org.cloudifysource.shell;

