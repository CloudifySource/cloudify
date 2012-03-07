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
package org.cloudifysource.dsl;

import java.util.Map;

import org.cloudifysource.dsl.context.ServiceContext;

/***********
 * All USM plugins implementation should implement this interface.
 * 
 * @author barakme
 * 
 */
public interface Plugin {

	/******************
	 * Setter for the Service Context of the current service.
	 * 
	 * @param context
	 *            the service context.
	 */
	void setServiceContext(ServiceContext context);

	/****************
	 * Setter for the plugin parameters, as defined in the Recipe file.
	 * 
	 * @param config
	 *            the plugin parameters.
	 */
	void setConfig(Map<String, Object> config);

}
