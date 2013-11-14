/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.rest.response;

import java.util.Map;

/**
 * A POJO representing a response to getMachinesDumpFile command via the REST Gateway. It holds a map by IP that holds
 * the data of the dump file of all the machines.
 * 
 * @see {@link org.cloudifysource.rest.controllers.ManagementController.getMachinesDumpFile(String, long)}
 * @author yael
 * @since 2.7.0
 */
public class GetMachinesDumpFileResponse {
	private Map<String, byte[]> dumpBytesPerIP;

	public Map<String, byte[]> getDumpBytesPerIP() {
		return dumpBytesPerIP;
	}

	public void setDumpBytesPerIP(final Map<String, byte[]> dumpBytesPerIP) {
		this.dumpBytesPerIP = dumpBytesPerIP;
	}

}
