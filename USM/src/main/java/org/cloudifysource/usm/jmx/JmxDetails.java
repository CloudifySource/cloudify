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

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;
import org.cloudifysource.usm.dsl.ServiceConfiguration;

/*************
 * A Details implementation that reads info from a JMX server.
 * 
 * @author barakme
 * @since 2.0
 * 
 */
public class JmxDetails extends AbstractJmxPlugin implements Details {

	@Override
	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final ServiceConfiguration config)
			throws DetailsException {

		return getJmxAttributes();

	}

}
