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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudTemplateHolder;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.fusesource.jansi.Ansi.Color;

/**
 * Adds templates to be included in the cloud's templates list. 
 * Reads the templates from the (groovy) templates-file.
 * 
 * Required arguments: 
 * 			templates-folder - Path to the folder contains the groovy templates file.
 * 
 * Command syntax: 
 * 			add-templates templatesFolder
 * 
 * @author yael
 * 
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "add-templates", description = "Adds templates to the cloud")
public class AddTemplates extends AdminAwareCommand {

	@Argument(required = true, name = "templates-folder", description = "The templates folder")
	private File templatesFolder;

	@Override
	protected Object doExecute() throws Exception {

		// validate templates folder and read the templates. 
		List<CloudTemplateHolder> cloudTemplatesFromFolder = validateTemplatesFolder();

		// add templates to cloud
		logger.info("Adding " + cloudTemplatesFromFolder.size() + " templates to cloud.");
		File zipFile = Packager.createZipFile("templates", templatesFolder);
		
		List<String> addedTempaltes = adminFacade.addTempaltes(zipFile);

		return getFormattedMessage("templates_added_successfully", Color.GREEN) 
				+ getFormatedAddedTemplateNamesList(addedTempaltes);
	}

	private List<CloudTemplateHolder> validateTemplatesFolder() throws CLIStatusException {
		logger.info("Validating tempaltes folder: " + templatesFolder.getName());
		if (!templatesFolder.exists()) {
			throw new CLIStatusException("templates_file_not_found", templatesFolder.getAbsolutePath());
		}
		if (!templatesFolder.isDirectory()) {
			throw new CLIStatusException("templates_file_not_a_file", templatesFolder.getAbsolutePath());
		}	
		File[] tempaltesFiles = DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATES_DSL_FILE_NAME_SUFFIX, templatesFolder);
		if (tempaltesFiles == null || tempaltesFiles.length == 0) {
			throw new CLIStatusException("templates_file_not_found", templatesFolder.getAbsolutePath());
		}
		List<CloudTemplateHolder> cloudTemplatesFromFolder = new LinkedList<CloudTemplateHolder>();
		for (File tempaltesFile : tempaltesFiles) {
			List<CloudTemplateHolder> cloudTemplatesFromFile = null;
			try {
				cloudTemplatesFromFile = ServiceReader.getCloudTemplatesFromFile(tempaltesFile);
			} catch (Exception e) {
				throw new CLIStatusException("read_dsl_file_failed", tempaltesFile.getAbsolutePath(), e.getMessage());
			}
			cloudTemplatesFromFolder.addAll(cloudTemplatesFromFile);
		}
		return cloudTemplatesFromFolder;
	}

	private static Object getFormatedAddedTemplateNamesList(final List<String> tempaltes) {
		StringBuilder sb = new StringBuilder();
		sb.append(CloudifyConstants.NEW_LINE)
		.append("Tamplates added:");
		for (String templateName : tempaltes) {
			sb.append(CloudifyConstants.NEW_LINE)
			.append(CloudifyConstants.TAB_CHAR)
			.append(templateName);
		}
		return sb;
	}

}
