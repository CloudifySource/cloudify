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
package org.cloudifysource.usm.details;

import java.util.Map;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;

/***************
 * Interface for classes that expose details to the service grid.
 * 
 * @author barakme
 * 
 */
public interface Details extends USMComponent {

	/*************
	 * Returns a map of static details. Details are collected using the GigaSpaces Service Grid and are available via
	 * the GigaSpaces Admin API.
	 * 
	 * @param usm the USM bean.
	 * @param config the initial configuration of the USM.
	 * @return The details.
	 * @throws DetailsException in case there was an error while generating the details.
	 */
	Map<String, Object> getDetails(UniversalServiceManagerBean usm, ServiceConfiguration config)
			throws DetailsException;

}
