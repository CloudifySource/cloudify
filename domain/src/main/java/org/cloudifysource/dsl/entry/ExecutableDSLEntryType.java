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

/*****************
 * Types of executable DSL entry types.
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public enum ExecutableDSLEntryType {

	/**********
	 * A groovy closure to be executed in process.
	 */
	CLOSURE,
	/**************
	 * A map where keys are regular expression mapping to OS names, and values are executable DSL entries.
	 */
	MAP,
	/*************
	 * A command line defined by its String parts, to be passed to the OS as a new process.
	 */
	LIST,
	/***********
	 * A command line to be executed by splitting the string using the space (' ') character to split the command into
	 * tokens.
	 */
	STRING
}
