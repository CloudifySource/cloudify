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
package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;

/**
 * Created with IntelliJ IDEA. User: elip Date: 5/22/13 Time: 2:54 PM <br>
 * </br>
 * 
 * Interface for installing/uninstalling services/application using the rest
 * gateway.
 */
public interface Installer {

	/**
	 * Executes a rest api call to install a specific service.
	 * 
	 * @param applicationName
	 *            The name of the application.
	 * @param serviceName
	 *            The name of the service to install.
	 * @param request
	 *            The install service request.
	 * @return The install service response.
	 * @throws Exception .
	 */
	InstallServiceResponse installService(final String applicationName, final String serviceName,
			final InstallServiceRequest request) throws Exception;

	/**
	 * Executes a rest api call to install an application.
	 * 
	 * @param applicationName
	 *            The name of the application.
	 * @param request
	 *            The install application request.
	 * @return The install application response.
	 * @throws Exception .
	 */
	InstallApplicationResponse installApplication(final String applicationName, final InstallApplicationRequest request)
			throws Exception;

	/**
	 * Executes a rest api call to uninstall an application.
	 * 
	 * @param applicationName
	 *            The name of the application.
	 * @param timeoutInMinutes
	 *            Timeout in minutes.
	 * @return The uninstall application response.
	 * @throws Exception .
	 */
	UninstallApplicationResponse uninstallApplication(final String applicationName, final int timeoutInMinutes)
			throws Exception;

	/**
	 * Executes a rest api call to uninstall a specific service.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param timeoutInMinutes
	 *            Timeout in minutes.
	 * @return The uninstall service response.
	 * @throws Exception .
	 */
	UninstallServiceResponse uninstallService(final String applicationName, final String serviceName,
			final int timeoutInMinutes) throws Exception;
}
