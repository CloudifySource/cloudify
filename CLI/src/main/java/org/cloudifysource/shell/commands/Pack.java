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
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;

/**
 * @deprecated
 * @author rafi, barakm
 * @since 2.0.0
 */
@Deprecated
@Command(name = "pack", scope = "cloudify", description = "Packs a recipe folder to a recipe archive")
public class Pack extends AbstractGSCommand {

	@Argument(required = true, name = "recipe-folder", description = "The recipe folder to pack")
	private File recipeFolder;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		final File packedFile = doPack(recipeFolder);
		return MessageFormat.format(messages.getString("packed_successfully"), packedFile.getAbsolutePath());
	}

	/**
	 * Packages the recipe files and other required files in a zip.
	 * @param recipeDirOrFile The recipe service DSL file or containing folder 
	 * @return A zip file
	 * @throws CLIException Reporting a failure to find or parse the given DSL file, or pack the zip file
	 */
	public static File doPack(final File recipeDirOrFile) throws CLIException {
		try {
			return Packager.pack(recipeDirOrFile);
		} catch (final IOException e) {
			throw new CLIException(e);
		} catch (final PackagingException e) {
			throw new CLIException(e);
		} catch (final DSLException e) {
			throw new CLIException(e);
		}
	}

}
