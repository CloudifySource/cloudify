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
package org.cloudifysource.usm.liveness;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.input.Tailer;
import org.cloudifysource.dsl.Plugin;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.AbstractUSMEventListener;

/**
 * FileLivenessDetector class is responsible for verifying that the process has finished loading by checking whether the
 * desired regex was found in the process's output log. The path to the process's log file is defined in the groovy
 * configuration file.
 * 
 * Using the FileLivenessDetector requires adding a plugin to the DSL file as following: plugins ([ plugin { name
 * "fileLiveness" className "org.cloudifysource.usm.liveness.FileLivenessDetector" config ([ "FilePath" :
 * System.getProperty("java.io.tmpdir") + "/groovyLog.log", "TimeoutInSeconds" : 30, "regularExpression" : "Hello_World"
 * ]) }, plugin {...
 * 
 * @author adaml
 * 
 */
public class FileLivenessDetector extends AbstractUSMEventListener implements LivenessDetector, Plugin {

	public static final String TIMEOUT_IN_SECONDS_KEY = "TimeoutInSeconds";
	public static final String REGULAR_EXPRESSION_KEY = "regularExpression";
	public static final String FILE_PATH_KEY = "FilePath";

	private static final Logger logger = Logger.getLogger(FileLivenessDetector.class.getName());

	private String filePath = "";
	private String regex = "";
	private int timeoutInSeconds = 60;

	private static final int TIMEOUT_BETWEEN_FILE_QUERYING = 1000;
	private String serviceDirectory;

	@Override
	public void setConfig(final Map<String, Object> config) {
		final String filePath = (String) config.get(FILE_PATH_KEY);
		if (filePath != null) {
			this.filePath = filePath;
		}

		final Integer timeout = (Integer)config.get(TIMEOUT_IN_SECONDS_KEY);
		if (timeout != null) {
			this.timeoutInSeconds = timeout;
		}
		final String regex = (String) config.get(REGULAR_EXPRESSION_KEY);
		if (regex != null) {
			this.regex = regex;
		}
	}

	/**
	 * isProcessAlive will sample the file defined in the groovy configuration file every second for the specified
	 * timeout period looking for a regex in the log that confirms the process has loaded successfully and return true
	 * if the regex was found.
	 * 
	 * @throws USMException .
	 * 
	 */
	@Override
	public boolean isProcessAlive()
			throws USMException {
		if (this.regex.isEmpty() || this.filePath.isEmpty()) {
			throw new USMException(
					"When using the FileLivnessDetector, both the file path and regex should be defined.");
		}
		File file = new File(this.filePath);
		if (!file.isAbsolute()) {
			file = new File(serviceDirectory, this.filePath);
		}
		final FileTailerListener listener = new FileTailerListener(this.regex);
		Tailer tailer = null;
		try {
			final long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() < startTime + TimeUnit.SECONDS.toMillis(timeoutInSeconds)) {
				if (file.exists()) {
					if (tailer == null) {
						tailer = Tailer.create(file, listener, TIMEOUT_BETWEEN_FILE_QUERYING, false);
					}
					if (listener.isProcessUp()) {
						logger.info("The regular expression " + this.regex + " was found in the process log");
						return true;
					}
				}
				try {
					Thread.sleep(TIMEOUT_BETWEEN_FILE_QUERYING);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (tailer != null) {
				tailer.stop();
			}
		}
		logger.info("The regular expression " + this.regex + " was NOT found in the process log");
		return false;

	}

	@Override
	public void setServiceContext(final ServiceContext context) {
		serviceDirectory = context.getServiceDirectory();
	}
}
