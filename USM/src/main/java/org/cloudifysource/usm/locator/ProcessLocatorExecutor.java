/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.usm.locator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.dsl.DSLEntryExecutor;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.launcher.ProcessLauncher;

/*************
 * A process locator implementation that delegates execution to a DSL entry, either a closure or an external script.
 *
 * @author barakme
 * @since 2.1.1
 *
 */
public class ProcessLocatorExecutor implements ProcessLocator {

	private final ExecutableDSLEntry locator;
	private final ProcessLauncher launcher;
	private final File puExtDir;

	/**************
	 * Constructor.
	 *
	 * @param locator
	 *            .
	 * @param launcher
	 *            .
	 * @param puExtDir
	 *            .
	 */
	public ProcessLocatorExecutor(final ExecutableDSLEntry locator, final ProcessLauncher launcher,
			final File puExtDir) {
		this.locator = locator;
		this.launcher = launcher;
		this.puExtDir = puExtDir;
	}

	@Override
	public List<Long> getProcessIDs()
			throws USMException {
		final EventResult result = new DSLEntryExecutor(locator, launcher, puExtDir).run();
		if (result.isSuccess()) {
			final Object retval = result.getResult();
			if (retval instanceof List<?>) {
				final List<?> closureResultList = (List<?>) retval;
				final List<Long> targetList = new ArrayList<Long>(closureResultList.size());
				for (final Object listItem : closureResultList) {
					if (listItem instanceof Long) {
						targetList.add((Long) listItem);
					} else if (listItem instanceof Integer) {
						targetList.add(Long.valueOf((Integer) listItem));
					} else if (listItem instanceof String) {
						try {
							final Long temp = Long.valueOf((String) listItem);
							targetList.add(temp);
						} catch (final NumberFormatException e) {
							throw new IllegalArgumentException(
									"Failed to parse a long value from locator result: " + listItem, e);
						}
					} else {
						throw new IllegalArgumentException(
								"Values in process locator result must be integers, "
										+ "longs or strings that can be parsed as longs");
					}
				}
				return targetList;
			} else if (retval instanceof String) {
				String listToParse = (String) retval;
				if (listToParse.startsWith("[") && listToParse.endsWith("]")) {
					listToParse = listToParse.substring(1, listToParse.length() - 1);
				}

				final String[] parts = listToParse.split(",");
				final List<Long> resultList = new ArrayList<Long>(parts.length);
				for (final String part : parts) {
					try {
						final Long pid = Long.valueOf(part);
						resultList.add(pid);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException(
								"Failed to parse PID list from locator output. Output was: " + retval
										+ ", failed to parse component: " + part, e);
					}
				}

				return resultList;
			} else {
				throw new IllegalArgumentException(
						"A process locator returned an unexpected result that is not a list of longs, "
								+ " or a comma aeparated list of longs. Result was of type: "
								+ retval.getClass().getName() + ".Result was: " + retval);
			}
		} else {
			throw new USMException("A Process locator failed to execute. Exception was: "
					+ result.getException(), result.getException());
		}

	}

}
