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

package org.cloudifysource.dsl.cloud;

/********
 * Supported remote execution modes.
 * @author barakme
 *
 */
public enum RemoteExecutionModes {
	/******
	 * Secure shell - typically used with Linux OS.
	 */
	SSH(22),
	/*******
	 * Windows Remote management.
	 */
	WINRM(5985);

	RemoteExecutionModes(final int port) {
		this.port = port;
	}

	private final int port;

	public int getDefaultPort() {
		return port;
	}



}
