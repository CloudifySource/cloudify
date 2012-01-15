/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.usm.launcher;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.USMException;


public interface ProcessLauncher extends USMComponent {

	// Process launch(UniversalServiceManagerConfiguration config, File
	// workingDir) throws USMException;

	String getCommandLine();

	// void run(String commandLine, File workingDir) throws USMException;

	Object launchProcess(final Object arg, final File workingDir)
		throws USMException;
	Object launchProcess(final Object arg, final File workingDir, final int retries, boolean redirectErrorStream, Map<String, Object> params)
			throws USMException;

	Process launchProcessAsync(final Object arg, final File workingDir, final File outputFile, final File errorFile)
	throws USMException;
	Process launchProcessAsync(final Object arg, final File workingDir, final int retries, boolean redirectErrorStream, List<String> params)
			throws USMException;

	Object launchProcess(Object arg, File workingDir, Map<String, Object> params)
			throws USMException;
}
