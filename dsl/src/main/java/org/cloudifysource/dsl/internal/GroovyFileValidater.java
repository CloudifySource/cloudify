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

package org.cloudifysource.dsl.internal;

import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;

import org.codehaus.groovy.control.CompilationFailedException;

/******************
 * Validates that groovy script files are valid, meaning there are no syntax errors in the files. Validation is performs
 * by parsing the file, effectively compiling it into a class file.
 * 
 * @author barakme
 * @since 2.2
 */
public class GroovyFileValidater {

	private final GroovyShell groovyShell;

	public GroovyFileValidater() {

		this.groovyShell = new GroovyShell(Thread.currentThread().getContextClassLoader());

	}

	/*************
	 * Validates the given file.
	 * 
	 * @param groovyFile the file to validate.
	 * @return the validation result.
	 */
	public GroovyFileCompilationResult validateFile(final File groovyFile) {
		try {

			this.groovyShell.parse(groovyFile);
			return GroovyFileCompilationResult.SUCCESS;
		} catch (final CompilationFailedException e) {

			return new GroovyFileCompilationResult(false, e.getMessage(), "Failed to parse groovy file "
					+ groovyFile.getName() + ": "
					+ e.getMessage(), e);

		} catch (final IOException e) {
			return new GroovyFileCompilationResult(false, e.getMessage(), "Failed to parse groovy file "
					+ groovyFile.getName() + ": "
					+ e.getMessage(), e);

		} catch (final Throwable t) {
			return new GroovyFileCompilationResult(false, t.getMessage(), "Failed to parse groovy file "
					+ groovyFile.getName() + ": "
					+ t.getMessage(), t);

		}
	}

}
