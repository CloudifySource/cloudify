/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

import org.cloudifysource.domain.entry.ExecutableDSLEntry;
import org.cloudifysource.domain.entry.ExecutableDSLEntryType;

/**
 * an executable entry holding a java command.
 * @author adaml
 *
 */
public class JavaExecutableEntry implements ExecutableDSLEntry {

	private Object command;
	
	@Override
	public ExecutableDSLEntryType getEntryType() {
		return ExecutableDSLEntryType.JAVA;
	}
	
	public Object getCommand() {
		return command;
	}
	
	public void setCommand(final Object command) {
		this.command = command;
	}

}
