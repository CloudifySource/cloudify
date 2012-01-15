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
package org.cloudifysource.restclient;