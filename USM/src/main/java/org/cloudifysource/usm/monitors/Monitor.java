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
package org.cloudifysource.usm.monitors;

import java.util.Map;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;

/**************
 * Interface for USM components that expose monitored statistics.
 * @author barakme
 * @since 2.2.0
 *
 */
public interface Monitor extends USMComponent {

	/**************
	 * Returns a map of statistics generated for this service. Statistics are collected using the GigaSpaces Service
	 * Grid and are available via the GigaSpaces Admin API.
	 * 
	 * @param usm The USM Bean.
	 * @param config The initial USM Configuration.
	 * @return the statistics.
	 * @throws MonitorException in case an error was encountered while generating the statistics.
	 */
	Map<String, Number> getMonitorValues(UniversalServiceManagerBean usm, ServiceConfiguration config)
			throws MonitorException;

}
