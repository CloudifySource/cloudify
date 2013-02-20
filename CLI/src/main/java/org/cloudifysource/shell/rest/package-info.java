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
 * 
 * <p>
 * {@link org.cloudifysource.shell.rest.RestAdminFacade} - Implements {@link org.cloudifysource.shell.AdminFacade}.
 * Communication with the REST server is done through the client {@link org.cloudifysource.restclient.GSRestClient},
 * using HTTP commands (GET, POST, DELETE).
 * <br>
 * The user executes connect command with a url address where the restful application is deployed.
 * The REST server is responsible to use the Admin API to perform the required logic.
 * 
 */
package org.cloudifysource.shell.rest;

