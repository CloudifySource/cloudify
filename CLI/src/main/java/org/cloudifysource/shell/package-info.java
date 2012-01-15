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
package org.cloudifysource.shell;

/**************************
 * Console/Shell
 * -------------
 * 
 * GigaShellMain - Extends the karaf framework Main object. This class is used to start the shell/console.
 * 
 * 
 * ConsoleWithProps - Extends the karaf framework Console Object. This class adds some branding and 
 * functionality on top of the base class, e.g., adds a default adminFacade to the session, 
 * overrides the get prompt method, set a default application and more.
 * 
 * 
 * 
 * Commands:
 * ---------
 * 
 * AbstractGSCommand - Implements the Action object of the karaf framework. Basically, this is the base class 
 * for each Command object we add. This class holds all of the relevant primitives and objects each command 
 * needs to use, e.g. CommandSession, messages(ResourceBundle)
 *  
 * 
 * AdminAwareCommand - Extends AbstractGSCommands. This is the base class for each Command Object that requires the 
 * user to execute the connect command before executing this command, i.e., to be connected to a rest server or to use 
 * the Admin API object. 
 * If the user didnt execute the connect command before using this command, 
 * the command wont be executed and a connect-first message will be returned.
 * 
 * 
 * All of other commands under org.cloudifysource.shell.commands extends either AbstractGSCommand,
 * e.g. Connect, SetAdmin, Pack, 
 * or extends AdminAwareCommand and delegate the work to the AdminFacade objects.
 * 
 * 
 * AdminFacade and REST:
 * ------------
 * 
 * AdminFacade - The Interface/API of the admin facade. 
 * The Admin facade is used by all of the commands to perform the required Admin API logic. 
 * 
 * 
 * AdminApiFacade - Implements AdminFacade by creating a local Admin API object.
 * 
 * 
 * RestAdminFacade - Implements AdminFacade using the commons httpClient and making http calls to a restful web application 
 * in a user specified server. The user executes connect command with a url address where the restful webapp is deployed, 
 * and this class communicates with the rest server. The rest server is responsible to use the Admin API to 
 * perform the required logic.
 * Currently, the RestAdminFacade uses MySSLSocketFactory which accepts all certificates.
 * When a valid certificate is issues, the block of code that uses MySSLSocketFactory needs to be removed(getSSLHttpClient method).
 * 
 * Communication with the REST server is done through the rest-client:
 * {@link org.cloudifysource.restclient.GSRestClient} using HTTP commands (GET, POST, DELETE).
 * 
 * 
 * 
 * Packaging:
 * ----------
 * 
 * Packager - Responsible to pack a user specified folder into a deployable zip. 
 * The packager expects the source folder to be with the following structure:
 * 
 * 		service.groovy
 * 		something.zip
 * 		install.sh
 * 		start.sh
 * 		...
 * 		usmlib(folder)
 * 			user-lib1.jar
 * 			user-lib2.jar
 *		...
 *
 * after executing the pack command a zip file is created with the following processing-unit structure:
 * 
 * 		ext
 * 			service.groovy
 * 			something.zip
 * 			install.sh
 * 			start.sh
 * 			...
 * 		lib
 *	 		usm.jar
 * 			user-lib1.jar
 * 			user-lib2.jar
 * 			...
 * 		META-INF
 * 			spring
 * 				pu.xml
 * 
 * 
 * @author rafip
 *****************************/
