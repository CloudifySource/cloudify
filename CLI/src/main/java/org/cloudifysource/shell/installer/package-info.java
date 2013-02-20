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
 * @author noak
 * @since 2.0.0
 * 
 *        <p>
 *        This package implements the installation, start-up and shutdown of management services and components.
 * 
 *        <p>
 *        {@link org.cloudifysource.shell.installer.AbstractManagementServiceInstaller} - This abstract is the
 *        skeleton of a management service installer, and includes the basic members that every management
 *        service installer use: {@link org.openspaces.admin.Admin}, a definition of memory quota, a service name
 *        and a zone name (might be identical to the service name).
 *        Installers extending this skeleton must implement:
 *        		<br>
 *        		- install()
 *        		<br>
 *        		- waitForInstallation(AdminFacade, GridServiceAgent, long, TimeUnit)
 *        
 *        <p>
 *        {@link org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper} - This class handles the start up
 *        and shutdown of the cloud components - management components (LUS, GSM, ESM), containers (GSCs) and an agent.
 *        
 *        <p>
 *        {@link org.cloudifysource.shell.installer.ManagementSpaceServiceInstaller} - Handles the installation of a
 *        management space.
 *        
 *        <p>
 *        {@link org.cloudifysource.shell.installer.ManagementWebServiceInstaller} - Handles the installation of a 
 *        management web service.
 *        
 *        <p>
 *        {@link org.cloudifysource.shell.installer.ConnectionLogsFilter} - The purpose of this class is to suppress
 *        communication errors while the agent is being bootstrapped or teared down.
 * 
 * 
 **/

package org.cloudifysource.shell.installer;