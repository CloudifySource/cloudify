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
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;

/**
 * @author barakm
 * @since 2.2.0
 * 
 *        Validates a service DSL file.
 * 
 */
@Command(name = "validate-service", scope = "cloudify", description = "Validates a service DSL file")
public class ValidateService extends AbstractGSCommand {

	@Argument(required = true, name = "file", description = "path to the DSL file or directory")
	private File serviceFile;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {
		if (!getServiceFile().exists()) {
			throw new CLIStatusException("service_file_doesnt_exist", serviceFile.getPath());
		}

		Service service = null;
		try {
			service = ServiceReader.readService(getServiceFile());

		} catch (final Exception e) {
			throw new CLIStatusException(e, "service_parsing_failure", e.getMessage());
		}

		return getFormattedMessage("service_parsing_success", service.getName());
	}

	public File getServiceFile() {
		return serviceFile;
	}

	public void setServiceFile(final File serviceFile) {
		this.serviceFile = serviceFile;
	}

}
