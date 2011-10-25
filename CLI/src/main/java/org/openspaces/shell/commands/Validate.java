/*******************************************************************************
 * /*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openspaces.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import java.io.File;
import java.text.MessageFormat;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(name = "validate", scope = "cloudify", description = "Validates the a DSL file")
public class Validate extends AbstractGSCommand {

	@Argument(required = true, name = "file", description = "path to the DSL file")
	File dsl;

	@Override
	protected Object doExecute() throws Exception {
		// TODO the shell framework doesnt support user input with '\'
		String absolutePath = dsl.getAbsolutePath();
		if (!dsl.exists()) {
			throw new IllegalArgumentException(MessageFormat.format(
					messages.getString("file_doesnt_exist"), absolutePath));
		}

		ServiceReader.getServiceFromFile(dsl, dsl.getParentFile());
		// PackagerUtils.getServiceFromFile(dsl);

		return MessageFormat.format(
				messages.getString("validated_successfully"), absolutePath);
	}
}
