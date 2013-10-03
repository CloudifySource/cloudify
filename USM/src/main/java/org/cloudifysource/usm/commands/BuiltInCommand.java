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
package org.cloudifysource.usm.commands;


/**
 * an interface for built-in command.
 * @author adaml
 *
 */
public interface BuiltInCommand {

	/**
	 * invoke the command with the specified parameters.
	 * @param params
	 * 		command parameters.
	 * @return
	 * 		result object.
	 */
	Object invoke(Object... params);
	
	/**
	 * returns the command name.
	 * @return
	 * 		the command name.
	 */
	String getName();
}
