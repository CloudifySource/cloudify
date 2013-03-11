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
 * InfraController provides Rest implementation for Infra. <br>
 * </br> 
 * <li>Hosts – the actually managed hosts</li> 
 * <li>Storage – the actual handlers for Storage</li>
 * <li>Load Balancer – the handler for LB and its groups</li>
 * 
 * 
 * 
 * @author ahmad
 * @since 2.5.0
 */
@Controller
@RequestMapping(value = "/{version}/infra")
public class InfraController extends BaseRestContoller {

	/******
	 * add group security.
	 * 
	 * @return
	 */
	@RequestMapping(value = "/securitygroups ", method = RequestMethod.POST)
	public void addSecurityGroup() {
		throwUnsupported();
	}

	/******
	 * add security rule.
	 * 
	 * @param groupName
	 *            the group name
	 * @return
	 */
	@RequestMapping(value = "/securitygroups/{groupName}", method = RequestMethod.POST)
	public void addSecurityRule(@PathVariable final String groupName) {
		throwUnsupported();
	}

	/******
	 * remove security rule.
	 * 
	 * @param groupName
	 *            the group name
	 * @return
	 */
	@RequestMapping(value = "/securitygroups/{groupName}", method = RequestMethod.DELETE)
	public void removeSecurityRule(@PathVariable final String groupName) {
		throwUnsupported();
	}

	/******
	 * update security rule.
	 * 
	 * @param groupName
	 *            the group name
	 * @return
	 */
	@RequestMapping(value = "/securitygroups/{groupName}", method = RequestMethod.PUT)
	public void updateSecurityRule(@PathVariable final String groupName) {
		throwUnsupported();
	}

	/******
	 * get security rule by name.
	 * 
	 * @param groupName
	 *            the group name
	 * 
	 */
	@RequestMapping(value = "/securitygroups/{groupName}", method = RequestMethod.GET)
	public void listSecurityRules(@PathVariable final String groupName) {
		throwUnsupported();
	}

	/******
	 * get all hosts.
	 * 
	 */
	@RequestMapping(value = "/hosts", method = RequestMethod.GET)
	public void getHosts() {
		throwUnsupported();
	}

	/******
	 * add BYON host.
	 * 
	 */
	@RequestMapping(value = "/hosts", method = RequestMethod.PUT)
	public void addHost() {
		throwUnsupported();
	}

	/******
	 * remove BYON host.
	 * 
	 */
	@RequestMapping(value = "/hosts", method = RequestMethod.DELETE)
	public void removeHost() {
		throwUnsupported();
	}

}
