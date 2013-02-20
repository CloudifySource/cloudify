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
package org.cloudifysource.shell.commands;

import java.io.File;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.ServiceReader;

/**
 * @author barakm
 * @since 2.2.0
 * 
 *        Validates the an application DSL file.
 * 
 */
@Command(name = "validate-application", scope = "cloudify", description = "Validates an application DSL file")
public class ValidateApplication extends AbstractGSCommand {

	@Argument(required = true, name = "file", description = "path to the DSL file")
	private File applicationFile;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {
		if (!getApplicationFile().exists()) {
			throw new CLIStatusException("application_not_found", getApplicationFile().getAbsolutePath());
		}

		Application application = null;
		try {
			application = ServiceReader.getApplicationFromFile(getApplicationFile()).getApplication();
		} catch (final Exception e) {
			throw new CLIStatusException(e, "application_parsing_failure", e.getMessage());
		}

		return getFormattedMessage("application_parsing_success", application.getName());
	}

	public File getApplicationFile() {
		return applicationFile;
	}

	public void setApplicationFile(final File applicationFile) {
		this.applicationFile = applicationFile;
	}
}
