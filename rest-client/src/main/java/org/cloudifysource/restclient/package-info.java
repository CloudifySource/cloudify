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
 ******************************************************************************/
package org.cloudifysource.restclient;
/**
 * Provides the classes necessary to communicate with the REST server.
 * The main class in this package is
 * {@link org.cloudifysource.restclient.GSRestClient}.
 * It uses httpcomponents-httpclient to issue HTTP requests and jackson mapper
 * to map json text to objects and vice versa.
 * GSRestClient communicates with the server using the methods GET,POST,DELETE.
 * These methods receive a relative URL (e.g., "service/services") and the rest 
 * client builds the full URL address based on that relative URL and on the URL
 * provided by the user through the "connect" command.
 * For example, if the user connects to http://localhost:8100 and executes GET
 * with "service/services" - the GSRestClient will issue a GET request with the
 * address: "http://localhost:8100/service/services" (same for POST & DELETE).
 * POST and DELETE can also receive a parameters map which will be embedded in
 * the HTTP request.
 * To send a file to the rest server use postFile.
 * 
 * Currently used by the CLI (command line interface) tool, 
 * through org.cloudifysource.shell.rest.RestAdminFacade.
 * <p>
 * 
 * Exception handling is performed with these classes:
 * {@link org.cloudifysource.restclient.RestException}
 * {@link org.cloudifysource.restclient.ErrorStatusException}
 * 
 */