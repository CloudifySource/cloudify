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
package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.cloudifysource.rest.command.CommandManager;
import org.cloudifysource.rest.out.OutputDispatcher;
import org.cloudifysource.rest.util.NotFoundHttpException;
import org.openspaces.admin.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring MVC controller for the RESTful Admin API via a reflection-based
 * implementation of dispatcher pattern
 * 
 * - Accepts a generic uri path which denotes a specific Admin request
 * 
 * - Parses and walks through the uri by activating getter methods to "dig into"
 * the admin object hierarchy
 * 
 * - Results marshaled as a generic document serialized to a JSON object
 * 
 * 
 * Usage examples: http://localhost:8099/admin/ElasticServiceManagers/Managers
 * http://localhost:8099/admin/GridServiceManagers/size
 * http://localhost:8099/admin/Spaces
 * http://localhost:8099/admin/VirtualMachines/VirtualMachines
 * http://localhost:8099/admin/VirtualMachines/VirtualMachines/3
 * http://localhost
 * :8099/admin/VirtualMachines/VirtualMachines/3/Statistics/Machine
 * /GridServiceAgents DETAILS:
 * http://localhost:8099/admin/GridServiceManagers/Uids
 * /49a6e2ef-5fd3-471a-94ff-c961a52ffd0f STATIST:
 * http://localhost:8099/admin/GridServiceManagers
 * /Uids/49a6e2ef-5fd3-471a-94ff-c961a52ffd0f
 * 
 * Note that the wiring and marshaling services are provided by Spring framework
 * 
 * Note 2: It is highly recommended that results will be viewed on FF with
 * JsonView plugin
 * 
 * @author giladh, adaml
 */

@Controller
@RequestMapping(value = "/admin/*")
public class AdminAPIController {

	@Autowired(required = true)
	private Admin admin;

	private static final Logger logger = Logger
			.getLogger(AdminAPIController.class.getName());

	/**
	 * redirects to index view.
	 * 
	 * @return a ModelAndView object with viewName = index
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView redirectToIndex() {
		return new ModelAndView("index");
	}

	/**
	 * REST GET requests handler wrapper.
	 * 
	 * @param httpServletRequest
	 *            The request
	 * @return The response as a map
	 * @throws Exception
	 *             Indicates the request failed
	 */
	@RequestMapping(value = "/**", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> get(final HttpServletRequest httpServletRequest)
			throws Exception {
		return getImplementation(httpServletRequest);
	}

	/**
	 * REST GET requests handler implementation Parses uri path activates
	 * appropriate getters serialize results into a document object and pass for
	 * JSON marshaling
	 * 
	 * uri type processing ============ ============== http:/../getArr/ind/...
	 * => (intermed.) resolve to arr[ind] and continue processing
	 * http:/../getMap/key/... => (intermed.) resolve to map.get(key) and
	 * continue processing http:/../getList/ind/... => (intermed.) resolve to
	 * list(ind) and continue processing http:/../getObj => (final) return obj
	 * fields (by public getters) http:/../getArr => (final) return arr.length
	 * http:/../getList => (final) return list.size() http:/../getMap => (final)
	 * return comma-separated list of map keys
	 * 
	 * 
	 */
	private Map<String, Object> getImplementation(
			final HttpServletRequest httpServletRequest) throws Exception {
		// admin acts as root
		final CommandManager manager = new CommandManager(httpServletRequest,
				getAdmin());
		manager.runCommands();
		final String hostAddress = getRemoteHostAddress(httpServletRequest);
		final String hostContext = httpServletRequest.getContextPath();
		return OutputDispatcher.outputResultObjectToMap(manager, hostAddress,
				hostContext);
	}

	private String getRemoteHostAddress(
			final HttpServletRequest httpServletRequest) {
		final String host = httpServletRequest.getServerName();
		final int port = httpServletRequest.getServerPort();
		return "http://" + host + ":" + port;
	}

	public Admin getAdmin() {
		return admin;
	}

	@ExceptionHandler(NotFoundHttpException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public void resolveNotFound(final Writer writer, final Exception e,
			final HttpServletRequest request) throws IOException {
		final String requestURL = request.getRequestURL().toString();
		logger.log(Level.INFO, "Cannot find URL: " + requestURL, e);
		writer.write("{\"status\":\"error\", \"error\":\""
				+ "Cannot find URL: " + requestURL + "cause: " + e.getMessage()
				+ "\"}");
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void resolveInternalServerError(final Writer writer,
			final Exception e) throws IOException {
		logger.log(Level.WARNING, "caught exception", e);
		writer.write("{\"status\":\"error\", \"error\":\"" + e.getMessage()
				+ "\"}");
	}
}
