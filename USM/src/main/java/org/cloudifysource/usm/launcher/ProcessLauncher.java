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

import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.USMException;

/***************
 * Interface for a process launcher component. A process launcher can process any of the following arguments: 1. Groovy
 * closure - in this case, a process will NOT be launcher. Instead, the closure will be executed in-process. The return
 * value of this execution will be the closure return value. 2. String - the string will be considered a command line an
 * executed. The return value of the invocation will be the String output from the combined system out and system error
 * of the process. If the process exit code is a value other then zero, an exception will be thrown, and the output will
 * be included in the exception message. 3. Map<String,String> - A map where the keys are regular expression of
 * Operating System names, and the values are String, which represent the command line to be executed.
 * 
 * TODO - some the overrides in this interface are no longer used - remove them.
 * 
 * 
 * @author barakme.
 * @since 2.0.0
 * 
 */
public interface ProcessLauncher extends USMComponent {

	/*************
	 * Returns the last executed command line, including all its modifications.
	 * 
	 * @return the last command line.
	 */
	String getCommandLine();

	/************
	 * Launch a process.
	 * 
	 * @param arg the process argument.
	 * @param workingDir the working directory for the process.
	 * @return the process result.
	 * @throws USMException if there was a problem launching the process, or the process did not terminate successfully.
	 */
	Object launchProcess(final ExecutableDSLEntry arg, final File workingDir)
			throws USMException;

	/*******************
	 * Launch a process.
	 * 
	 * @param arg The process argument.
	 * @param workingDir the working directory where the process will be executed.
	 * @param retries number of retries.
	 * @param redirectErrorStream should the error stream be redirected to the output stream.
	 * @param params parameters to pass to the process.
	 * @return the process result.
	 * @throws USMException if there was a problem launching the process, or the process did not terminate successfully.
	 */
	Object launchProcess(final ExecutableDSLEntry arg, final File workingDir,
			final int retries, boolean redirectErrorStream,
			Map<String, Object> params)
			throws USMException;

	/**********
	 * Launch a process asynchronously, without waiting for it to terminate.
	 * 
	 * @param arg the process argument.
	 * @param workingDir the working directory for the process.
	 * @param outputFile the file where the process output stream will be redirected.
	 * @param errorFile the file where the process error stream will be redirected.
	 * @return the process handle.
	 * @throws USMException if there was a problem launching the process.
	 */
	Process launchProcessAsync(final ExecutableDSLEntry arg, final File workingDir, final File outputFile,
			final File errorFile)
			throws USMException;

	/***********************
	 * Launch a process asynchronously, without waiting for it to terminate.
	 * 
	 * @param arg the process argument.
	 * @param workingDir the working directory for the process.
	 * @param retries number of retries.
	 * @param redirectErrorStream should the output stream be redirected to the standard output.
	 * @param params the process parameters.
	 * @return the process handle.
	 * @throws USMException if there was a problem launching the process.
	 */
	Process launchProcessAsync(final ExecutableDSLEntry arg, final File workingDir, final int retries,
			boolean redirectErrorStream,
			List<String> params)
			throws USMException;

	/*********************
	 * Launches a process.
	 * 
	 * @param arg the process argument.
	 * @param workingDir the process working directory.
	 * @param params the process parameters.
	 * @return the process result.
	 * @throws USMException if there was a problem launching the process, or the process did not terminate successfully.
	 */
	Object launchProcess(ExecutableDSLEntry arg, File workingDir, Map<String, Object> params)
			throws USMException;
}
