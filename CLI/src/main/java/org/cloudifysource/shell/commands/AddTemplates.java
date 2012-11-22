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
import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudTemplatesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.fusesource.jansi.Ansi.Color;

/**
 * Adds templates to be included in the cloud's templates list. 
 * Reads the templates from the (groovy) templates-file.
 * 
 * Required arguments: 
 * 			templates-file-or-folder - Path to a single groovy file (one template to add)
 * 										or to a folder (zipped or not) contains one or more groovy files
 * 										each groovy file has the form of "*-template.groovy" 
 * 										and declare one template to add. 
 * 
 * Command syntax: 
 * 			add-templates templates-file-or-folder
 * 
 * @author yael
 * 
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "add-templates", description = "Adds templates to the cloud")
public class AddTemplates extends AdminAwareCommand {

	@Argument(required = true, name = "templates-file-or-folder", 
			description = "A template file or a tampltes folder that can contains several template files.")
	private File templatesFileOrDir;

	@Override
	protected Object doExecute() throws Exception {

		// validate templates folder. 
		String templatesFolderName = templatesFileOrDir.getName();
		String templatesPath = templatesFileOrDir.getAbsolutePath();
		logger.info("Validating tempaltes folder and files at: " + templatesPath);
		if (!templatesFileOrDir.exists()) {
			throw new CLIStatusException("templates_file_not_found", templatesPath);
		}
		int numTemplatesInFolder = 0;
		File zipFile = templatesFileOrDir;

		if (templatesFileOrDir.isFile()) {
			if (templatesFolderName.endsWith(".zip") || templatesFolderName.endsWith(".jar")) {
				try {
					numTemplatesInFolder = CloudTemplatesReader.readCloudTemplatesFromZip(templatesFileOrDir).size();
				} catch (DSLException e) {
					throw new CLIStatusException("read_dsl_file_failed", templatesPath, e.getMessage());
				}
			} else { 
				// templatesFileOrDir is a groovy file
				if (!templatesFolderName.endsWith(DSLUtils.TEMPLATES_DSL_FILE_NAME_SUFFIX)) {
					throw new CLIStatusException("illegal_template_file_name", templatesFolderName);
				}
				File parentFile = templatesFileOrDir.getParentFile();
				File[] actualTemplatesDslFiles =
						DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATES_DSL_FILE_NAME_SUFFIX, parentFile);
				if (actualTemplatesDslFiles.length > 1) {
					throw new CLIStatusException("too_many_template_files", Arrays.toString(actualTemplatesDslFiles));
				}
				try {
					numTemplatesInFolder = CloudTemplatesReader.readCloudTemplatesFromFile(templatesFileOrDir).size();
				} catch (DSLException e) {
					throw new CLIStatusException("read_dsl_file_failed", templatesPath, e.getMessage());
				}
				zipFile = Packager.createZipFile("templates", parentFile);
			}
		} else {
			// templatesFileOrDir is a directory
			try {
				numTemplatesInFolder = CloudTemplatesReader.readCloudTemplatesFromDirectory(templatesFileOrDir).size();
			} catch (DSLException e) {
				throw new CLIStatusException("read_dsl_file_failed", templatesPath, 
						e.getMessage());
			}
			zipFile = Packager.createZipFile("templates", templatesFileOrDir);
		}

		// add the templates to the cloud
		if (numTemplatesInFolder == 0) {
			throw new CLIStatusException("no_template_files", templatesPath);
		}
		logger.info("Adding " + numTemplatesInFolder + " templates to cloud.");
		List<String> addedTempaltes = adminFacade.addTempaltes(zipFile);

		return getFormattedMessage("templates_added_successfully", Color.GREEN) 
				+ getFormatedAddedTemplateNamesList(addedTempaltes);
	}
	
	private static Object getFormatedAddedTemplateNamesList(final List<String> tempaltes) {
		StringBuilder sb = new StringBuilder();
		sb.append(CloudifyConstants.NEW_LINE)
		.append("Added " + tempaltes.size() + " tempaltes:");
		
		for (String templateName : tempaltes) {
			sb.append(CloudifyConstants.NEW_LINE)
			.append(CloudifyConstants.TAB_CHAR)
			.append(templateName);
		}
		return sb;
	}

}
