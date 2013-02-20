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

/**
 * @author rafip, noak
 * @since 2.0.0
 * <p>
 * 
 * <h4>Commands</h4>
 * {@link org.cloudifysource.shell.commands.AbstractGSCommand} - Implements the Action object of the karaf framework.
 * <br>
 * This is the base class for each Command object we add. This class holds all of the relevant primitives and objects
 * each command needs to use (e.g. CommandSession, messages resource bundle).
 * <p>
 * 
 * {@link org.cloudifysource.shell.commands.AdminAwareCommand} - Extends 
 * {@link org.cloudifysource.shell.commands.AbstractGSCommand}.
 * <br>
 * This is the base class for each Command Object that requires a connection to a rest server or to use the Admin API
 * object. If the user did not execute the {@link org.cloudifysource.shell.commands.Connect} command before using this
 * command, the command will not be executed and an error message will be returned.
 * <p>
 * 
 * The commands can extend one of the following classes:<br>
 * 1. org.apache.karaf.shell.console.AbstractAction (e.g. {@link org.cloudifysource.shell.commands.Quit})<br>
 * 2. {@link org.cloudifysource.shell.commands.AbstractGSCommand} 
 * (e.g. {@link org.cloudifysource.shell.commands.Connect})<br>
 * 3. {@link org.cloudifysource.shell.commands.AdminAwareCommand} 
 * (e.g. {@link org.cloudifysource.shell.commands.InstallService}).
 * <p>
 * 
 * <h4>Exception handling</h4>
 * Exception handling is performed using {@link org.cloudifysource.shell.commands.CLIException} as a basic CLI
 * exception, or {@link org.cloudifysource.shell.commands.CLIStatusException} for more detailed exceptions with a reason
 * code and optionally arguments to be passed to the message formatter.
 * 
 */

package org.cloudifysource.shell.commands;
