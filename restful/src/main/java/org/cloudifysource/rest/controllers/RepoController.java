/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 
 * RepoController provides Rest implementation for The Recipes stored for users to use. <br>
 * 
 * 
 * @author ahmad
 * @since 2.5.0
 */
@Controller
@RequestMapping(value = "/{version}/repo")
public class RepoController extends BaseRestContoller {

	
	/******
	 * push application recipe .
	 * 
	 */
	@RequestMapping(value = "/apps", method = RequestMethod.POST)
	public void pushAppRecipe() {
			throwUnsupported();
	}
	
	
	/******
	 * get application recipe given name.
	 * @param appName the application name
	 * @return
	 */
	@RequestMapping(value = "/apps/{appName}", method = RequestMethod.GET)
	public void getAppRecipe(
			@PathVariable final String appName) {
			throwUnsupported();
	}
	
	/******
	 * update application recipe given name.
	 * @param appName the application name
	 * @return
	 */
	@RequestMapping(value = "/apps/{appName}", method = RequestMethod.PUT)
	public void updateAppRecipe(
			@PathVariable final String appName) {
			throwUnsupported();
	}
	
	
	/******
	 * delete application recipe given name.
	 * @param appName the application name
	 * @return
	 */
	@RequestMapping(value = "/apps/{appName}", method = RequestMethod.DELETE)
	public void deleteAppRecipe(
			@PathVariable final String appName) {
			throwUnsupported();
	}
	
	
}
