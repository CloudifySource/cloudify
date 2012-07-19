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
package org.cloudifysource.usm.jmx;

import java.util.Map;

import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.ServiceConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;

/************
 * A monitor implementation that reads info from a JMX server.
 * 
 * @author barakme
 * 
 */
public class JmxMonitor extends AbstractJmxPlugin implements Monitor {

	@Override
	public Map<String, Number> getMonitorValues(final UniversalServiceManagerBean usm,
			final ServiceConfiguration config)
			throws MonitorException {

		final Map<String, Object> jmxAttributes = getJmxAttributes();

		return USMUtils.convertMapToNumericValues(jmxAttributes);
	}
}
