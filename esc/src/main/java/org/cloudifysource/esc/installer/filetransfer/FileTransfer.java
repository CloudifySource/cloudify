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

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/******
 * Interface for file transfer implementation.
 * 
 * @author barakme
 * @since 2.5.0
 * 
 */
public interface FileTransfer {

	/************
	 * Copies the required files from the local directory.
	 * 
	 * @param details
	 *            the installation details.
	 * @param excludedFiles
	 *            files to be excluded from the copy operation.
	 * @param additionalFiles
	 *            additional files to be copied that are not in the local
	 *            directory.
	 * @param endTimeMillis
	 *            timeout time for this operation.
	 * @throws TimeoutException
	 *             if the timeout target time is exceeded.
	 * @throws InstallerException
	 *             if there was a problem.
	 */
	void copyFiles(InstallationDetails details, Set<String> excludedFiles, List<File> additionalFiles,
			long endTimeMillis)
					throws TimeoutException, InstallerException;

	/**********
	 * Initializes the file transfer implementation.
	 * 
	 * @param details
	 *            the installation details.
	 * @param endTimeMillis
	 *            the target end time.
	 * @throws TimeoutException
	 *             if the timeout was exceeded.
	 * @throws InstallerException
	 *             if there was a problem.
	 */
	void initialize(final InstallationDetails details, final long endTimeMillis)
			throws TimeoutException, InstallerException;

}