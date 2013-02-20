/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.dsl.entry;

import java.util.LinkedHashMap;

/***************
 * An executable entry that runs an operating system command line defined as a Map where each key is a regex for an
 * operating system, and the value is a command.
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public class MapExecutableEntry extends LinkedHashMap<String, ExecutableDSLEntry> implements ExecutableDSLEntry {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public ExecutableDSLEntryType getEntryType() {
		return ExecutableDSLEntryType.MAP;
	}

}
