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
package org.cloudifysource.usm.dsl;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;



public class DSLDetails implements Details {

	public Map<String, Object> getDetails(UniversalServiceManagerBean usm, UniversalServiceManagerConfiguration config)
			throws DetailsException {
		Service service = ((DSLConfiguration)config).getService();
		HashMap<String, Object> map = new HashMap<String,Object>();

		map.put("icon", service.getIcon());
		//map.put("type", service.getType());
		//map.put("protocolDescription", service.getNetwork() == null ? null : service.getNetwork().getProtocolDescription());
		
		
		return map;
	}

}
