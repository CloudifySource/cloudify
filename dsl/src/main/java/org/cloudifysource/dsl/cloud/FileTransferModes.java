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
 * Supported file transfer modes.
 * 
 * @author barakme
 *
 */
public enum FileTransferModes {

	/*****
	 * Secure FTP. Typically used for linux.
	 */
	SFTP(22),
	
	/*******
	 * Secure copy. Used for linux when SFTP is not enabled.
	 */
	SCP(22),
	/*******
	 * Windows file sharing.
	 */
	CIFS(445);
	
	private final int defaultPort;
	
	private FileTransferModes(final int defaultPort) {
		this.defaultPort = defaultPort;
	}

	public int getDefaultPort() {
		return defaultPort;
	}
}
