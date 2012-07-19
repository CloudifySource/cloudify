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
package org.cloudifysource.usm.dsl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.launcher.ProcessLauncher;

/*********
 * Executes a DSL entry, using the provided process launcher implementation.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
public class DSLEntryExecutor {

	private final ExecutableDSLEntry entry;
	private final ProcessLauncher launcher;
	private final File workDir;
	private final Map<String, Object> params;

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DSLEntryExecutor.class
			.getName());

	/********
	 * Constructor. 
	 * @param entry .
	 * @param launcher .
	 * @param workDir .
	 */
	public DSLEntryExecutor(final ExecutableDSLEntry entry, final ProcessLauncher launcher, final File workDir) {
		this(entry, launcher, workDir, new HashMap<String, Object>());
	}

	/********
	 * Constructor. 
	 * @param entry .
	 * @param launcher .
	 * @param workDir .
	 * @param params .
	 */
	public DSLEntryExecutor(final ExecutableDSLEntry entry, final ProcessLauncher launcher, final File workDir,
			final Map<String, Object> params) {
		this.entry = entry;
		this.launcher = launcher;
		this.workDir = workDir;
		this.params = params;
	}

	/*******
	 * Executers the DSL Entry.
	 * @return the event result.
	 */
	public EventResult run() {
		if (entry == null) {
			return EventResult.SUCCESS;
		}
		try {
			final Object result = launcher.launchProcess(entry,
					workDir,
					params);
			return new EventResult(result);
		} catch (final USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute entry: " + entry,
					e);
			return new EventResult(e);
		}

	}

}
