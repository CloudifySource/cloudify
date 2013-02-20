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

package org.cloudifysource.dsl.entry;

import groovy.lang.Closure;
import groovy.lang.GString;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cloudifysource.dsl.internal.DSLValidationException;

/***********
 * Factory class for creating an executable DSL entry from a DSL value.
 *
 * @author barakme
 * @since 2.2.0
 *
 */
public final class ExecutableDSLEntryFactory {

	// private static GroovyFileValidater groovyValidater = new GroovyFileValidater();

	private ExecutableDSLEntryFactory() {
		// private constructor to prevent instantiation
	}

	/************
	 * Creates a map of executable entries, keyed by the name of the entry. Useful for custom commands, though it may
	 * come in handy elsewhere.
	 *
	 * @param arg
	 *            the input value.
	 * @param entryName
	 *            the entry name.
	 * @param workDirectory
	 *            the work directory for this entry.
	 * @return the executable entries map.
	 * @throws DSLValidationException
	 *             if an entry is invalid.
	 */
	public static ExecutableEntriesMap createEntriesMap(final Object arg, final Object entryName,
			final File workDirectory)
			throws DSLValidationException {

		final ExecutableEntriesMap result = new ExecutableEntriesMap();
		copyElementsToEntriesMap(arg, entryName, result, workDirectory);
		return result;

	}

	// TODO - re-enable this to activate groovy file validations

//	private static void
//			validateStringEntry(final StringExecutableEntry stringExecutableEntry, final File workDirectory) {
//		final String command = stringExecutableEntry.getCommand();
//		final String[] parts = command.split(" ");
//		final String fileName = parts[0];
//		if (fileName.endsWith(".groovy")) {
//			File file = new File(fileName);
//			if (!file.isAbsolute()) {
//				file = new File(workDirectory, fileName);
//			}
//
//			// if (file.exists() && file.isFile()) {
//			// GroovyFileCompilationResult result = groovyValidater.validateFile(file);
//			// if (!result.isSuccess()) {
//			// throw new DSLValidationException(result.getErrorMessage(), result.getCause());
//			// }
//			// }
//		}
//
//	}

	/****************
	 * Created an executable entry from a DSL value. The argument must be from one of the supported types.
	 *
	 * @param arg
	 *            the dsl value.
	 * @param entryName
	 *            the dsl entry name.
	 * @param workDirectory
	 *            The directory where the DSL is being processed.
	 * @return the executable entry wrapper for the given arg.
	 * @throws DSLValidationException
	 *             if the entry is invalid.
	 */
	@SuppressWarnings("unchecked")
	public static ExecutableDSLEntry createEntry(final Object arg, final Object entryName, final File workDirectory)
			throws DSLValidationException {

		if (arg == null) {
			// this might be useful in service extension, where the child service wants to remove a command added by the
			// parent service
			return null;
		}
		if (arg instanceof Closure<?>) {
			return new ClosureExecutableEntry((Closure<?>) arg);
		}

		if (arg instanceof String) {
			final StringExecutableEntry stringExecutableEntry = new StringExecutableEntry((String) arg);
			//validateStringEntry(stringExecutableEntry, workDirectory);
			return stringExecutableEntry;
		} else if (arg instanceof GString) {
			return new StringExecutableEntry(arg.toString());
		} else if (arg instanceof List<?>) {
			return new ListExecutableEntry((List<String>) arg);
		} else if (arg instanceof Map<?, ?>) {

			// verify types of keys and objects, and create a new map with wrapper entry objects for each value.
			final MapExecutableEntry result = new MapExecutableEntry();
			copyElementsToEntriesMap(arg, entryName, result, workDirectory);
			return result;
		}
		throw new IllegalArgumentException("The entry: " + entryName
				+ " is not a valid executable entry: The given value: " + arg + " is of type: "
				+ arg.getClass().getName() + " which is not a valid type for an executable entry");

	}

	private static void copyElementsToEntriesMap(final Object arg, final Object entryName,
			final Map<String, ExecutableDSLEntry> result, final File workDirectory)
			throws DSLValidationException {
		@SuppressWarnings("unchecked")
		final Map<Object, Object> originalMap = (Map<Object, Object>) arg;
		final Set<Entry<Object, Object>> entries = originalMap.entrySet();

		for (final Entry<Object, Object> entry : entries) {
			Object key = entry.getKey();
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Entry " + entryName
						+ " has a sub entry key which is not a string. Subentry was: " + key);
			}
			// RECURSIVE CALL!
			final ExecutableDSLEntry executableEntry = createEntry(entry.getValue(), key, workDirectory);
			result.put((String) key, executableEntry);

		}
	}

}
