/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/****************
 * .
 * @author itaif
 *
 */
public class DefaultProvisioningDriverClassContext implements ProvisioningDriverClassContext {

	private final Map<String, Object> context = new HashMap<String, Object>();

	@Override
	public Object getOrCreate(final String key, final Callable<Object> factory) throws Exception {
		synchronized (context) {
			if (!context.containsKey(key)) {
				final Object value = factory.call();
				context.put(key, value);
				return value;
			}
			return context.get(key);
		}
	}

}
