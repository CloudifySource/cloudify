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

/**************************
 * RestAdminFacade - Implements AdminFacade using the commons httpClient and making http calls to a restful web
 * application in a user specified server. The user executes connect command with a url address where the restful
 * webapp is deployed, and this class communicates with the rest server. The rest server is responsible to use the
 * Admin API to perform the required logic.
 * Currently, the RestAdminFacade uses MySSLSocketFactory which accepts all certificates.
 * When a valid certificate is issues, the block of code that uses MySSLSocketFactory needs to be removed 
 * (getSSLHttpClient method).
 * 
 * Communication with the REST server is done through the rest-client:
 * {@link org.cloudifysource.restclient.GSRestClient} using HTTP commands (GET, POST, DELETE).
 */

