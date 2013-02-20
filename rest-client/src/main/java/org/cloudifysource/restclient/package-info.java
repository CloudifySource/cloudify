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
 * @author noak
 * @since 2.0.0
 * <p>
 * 
 * This package provides the classes necessary to communicate with the REST server.
 * <br>
 * The main class in this package is {@link org.cloudifysource.restclient.GSRestClient}.
 * It communicates with the REST server using standard HTTP commands (i.e. Get, Post, Delete).
 * Requests are parsed using jackson mapper to map json text to objects and vice versa.
 * <br>
 * The client receives relative URLs and builds the full URL address based on the URL
 * provided by the user initially, through the "connect" command.
 * <br>
 * (For example, if the user connects to http://localhost:8100 and executes GET
 * with "service/services" - the GSRestClient will issue a GET request to:
 * "http://localhost:8100/service/services").
 * <br>
 * POST and DELETE can also receive a map of parameters which will be embedded in
 * the HTTP request sent to the server.
 * <br>
 * To send a file to the rest server use postFile.
 * <p>
 * 
 * Exception handling is performed with these classes:
 * <br>
 * {@link org.cloudifysource.restclient.RestException}
 * <br>
 * {@link org.cloudifysource.restclient.ErrorStatusException}
 * <p>
 * 
 * {@link org.cloudifysource.restclient.RestSSLSocketFactory} - Handles secure communication with the REST server.
 * 
 */

package org.cloudifysource.restclient;