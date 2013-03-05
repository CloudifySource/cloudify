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
