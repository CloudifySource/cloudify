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
package org.cloudifysource.dsl.rest.response;

/**
 * A POJO representing a response to getMachineDumpFile command via the REST Gateway. 
 * It holds the data of the zip file containing the dump of the machine.
 * 
 * @see {@link org.cloudifysource.rest.controllers.ManagementController.getMachineDumpFile(String, String, long)}
 * @author yael
 * @since 2.7.0
 */
public class GetMachineDumpFileResponse {

	private byte[] dumpBytes;

	public byte[] getDumpBytes() {
		return dumpBytes;
	}

	public void setDumpBytes(final byte[] dumpBytes) {
		this.dumpBytes = dumpBytes;
	}
	
}
