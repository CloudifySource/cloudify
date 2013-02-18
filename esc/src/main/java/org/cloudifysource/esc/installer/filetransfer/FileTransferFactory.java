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

package org.cloudifysource.esc.installer.filetransfer;

import org.cloudifysource.dsl.cloud.FileTransferModes;

/**********
 * Factory class for file transfer implementations, used by the agentless installer.
 * @author barakme
 *
 */
public final class FileTransferFactory {

	/*********
	 * Private constructor to prevent construvtion.
	 */
	private FileTransferFactory() {

	}

	/**********
	 * Factory method, returning a file transfer implementation.
	 * @param mode the required mode.
	 * @return the implementing object.
	 */
	public static FileTransfer getFileTrasnferProvider(final FileTransferModes mode) {
		switch (mode) {
		case CIFS:
			return new CifsFileTransfer();
		case SFTP:
			return new SftpFileTransfer();
		case SCP:
			return new ScpFileTransfer();
		default:
			throw new UnsupportedOperationException("Unsupported file transfer mode: " + mode);
		}
	}
}
