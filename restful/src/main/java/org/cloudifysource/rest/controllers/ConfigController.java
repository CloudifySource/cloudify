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
 * ConfigRestController provides Rest implementation for Configuration.
 * 
 * 
 * @author ahmad
 *
 */
@Controller
@RequestMapping(value = "/{version}/config")
public class ConfigController extends BaseRestContoller {
	
	
	/******
	 * add compute template.
	 * 
	 */
	@RequestMapping(value = "/compute", method = RequestMethod.POST)
	public void addComputeTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * get all compute template.
	 * 
	 */
	@RequestMapping(value = "/compute", method = RequestMethod.GET)
	public void listComputeTemplates() {
			throwUnsupported();
	}
	
	/******
	 * delete compute template.
	 * 
	 */
	@RequestMapping(value = "/compute", method = RequestMethod.DELETE)
	public void deleteComputeTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * add network template.
	 * 
	 */
	@RequestMapping(value = "/network", method = RequestMethod.POST)
	public void addNetworkTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * get all network template.
	 * @param networkName the network name
	 *  
	 */
	@RequestMapping(value = "/network/{networkName}", method = RequestMethod.GET)
	public void listNetworkTemplates(
			@PathVariable final String networkName) {
			throwUnsupported();
	}
	
	
	/******
	 * delete network template.
	 * @param networkName the network name
	 *  
	 */
	@RequestMapping(value = "/network/{networkName}", method = RequestMethod.DELETE)
	public void deleteNetworkTemplates(
			@PathVariable final String networkName) {
			throwUnsupported();
	}
	
	
	/******
	 * add load balancer template.
	 * 
	 */
	@RequestMapping(value = "/loadbalancer", method = RequestMethod.POST)
	public void addLBTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * get all load balancer templates.
	 * 
	 */
	@RequestMapping(value = "/loadbalancer", method = RequestMethod.GET)
	public void listLBTemplate() {
			throwUnsupported();
	}
	
	/******
	 * delete load balancer templates.
	 * 
	 */
	@RequestMapping(value = "/loadbalancer", method = RequestMethod.DELETE)
	public void deleteLBTemplate() {
			throwUnsupported();
	}
	
	/******
	 * add storage templates.
	 * 
	 */
	@RequestMapping(value = "/storage", method = RequestMethod.POST)
	public void addStorageTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * get all storage templates.
	 * 
	 */
	@RequestMapping(value = "/storage", method = RequestMethod.GET)
	public void listStorageTemplate() {
			throwUnsupported();
	}
	
	
	/******
	 * delete storage templates.
	 * 
	 */
	@RequestMapping(value = "/storage", method = RequestMethod.DELETE)
	public void deleteStorageTemplate() {
			throwUnsupported();
	}
	
	
	
	/******
	 * add security group template.
	 * 
	 */
	@RequestMapping(value = "/securitygroups", method = RequestMethod.POST)
	public void addSecurityGrouptemplate() {
			throwUnsupported();
	}
	

}
