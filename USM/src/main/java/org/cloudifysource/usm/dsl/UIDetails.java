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

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;
import org.openspaces.ui.UserInterface;

/**************
 * A USM details implementation the exposes the UI POJO as a Detail.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class UIDetails implements Details {

	private final UserInterface ui;

	/**********
	 * .
	 * 
	 * @param ui .
	 */
	public UIDetails(final UserInterface ui) {
		super();
		this.ui = ui;
	}

	@Override
	public Map<String, Object> getDetails(final UniversalServiceManagerBean usm,
			final ServiceConfiguration config)
			throws DetailsException {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put("USM.UI", ui);
		return map;
	}

}
