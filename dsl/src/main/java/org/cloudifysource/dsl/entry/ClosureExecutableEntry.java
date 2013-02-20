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

import groovy.lang.Closure;


/***************
 * An executable entry that runs a Groovy Closure.
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public class ClosureExecutableEntry implements ExecutableDSLEntry {

	private Closure<?> command;

	public ClosureExecutableEntry(final Closure<?>  command) {
		super();
		this.command = command;
	}

	public Closure<?>  getCommand() {
		return command;
	}

	public void setCommand(final Closure<?>  command) {
		this.command = command;
	}

	@Override
	public ExecutableDSLEntryType getEntryType() {
		return ExecutableDSLEntryType.CLOSURE;
	}

	@Override
	public String toString() {
		return "Closure: " + super.toString();
	}
}
