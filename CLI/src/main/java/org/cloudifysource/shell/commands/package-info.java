/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.commands;

/**
 * Commands
 * --------
 * AbstractGSCommand - Implements the Action object of the karaf framework. Basically, this is the base class
 * for each Command object we add. This class holds all of the relevant primitives and objects each command
 * needs to use, e.g. CommandSession, messages(ResourceBundle)
 * 
 * 
 * AdminAwareCommand - Extends AbstractGSCommands. This is the base class for each Command Object that
 * requires the user to execute the connect command before executing this command, i.e., to be connected to a
 * rest server or to use the Admin API object. If the user didnt execute the connect command before using this
 * command, the command wont be executed and a connect-first message will be returned.
 * 
 * 
 * All of other commands under org.cloudifysource.shell.commands extends either
 * {@link org.cloudifysource.shell.commands.AbstractGSCommand} (e.g. Connect, TestRecipe, StartAgent) or extends
 * {@link org.cloudifysource.shell.commands.AdminAwareCommand} and delegate the work to the AdminFacade objects.
 * 
 * Exceptions
 * ----------
 * Exception handling is performed using {@link org.cloudifysource.shell.commands.CLIException} as a basic CLI
 * exception, or {@link org.cloudifysource.shell.commands.CLIStatusException} for details exceptions with a reason
 * code and optionally arguments to be passed to the message formatter.
 */
