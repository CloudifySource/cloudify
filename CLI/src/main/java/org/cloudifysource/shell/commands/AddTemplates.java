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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.ComputeTemplatesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
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
			description = "A template file or a templates folder that can contain several template files.")
	private File templatesFileOrDir;

	@Override
	protected Object doExecute() throws Exception {

		// validate templates folder. 
		String templatesFolderName = templatesFileOrDir.getName();
		String templatesPath = templatesFileOrDir.getAbsolutePath();
		logger.info("Validating template folder and files: " + templatesPath);
		if (!templatesFileOrDir.exists()) {
			throw new CLIStatusException("templates_file_not_found", templatesPath);
		}
		int numTemplatesInFolder;
		File zipFile = templatesFileOrDir;
		ComputeTemplatesReader reader = new ComputeTemplatesReader();

		if (templatesFileOrDir.isFile()) {
			if (templatesFolderName.endsWith(".zip") || templatesFolderName.endsWith(".jar")) {
				try {
					numTemplatesInFolder = reader.readCloudTemplatesFromZip(templatesFileOrDir).size();
				} catch (DSLException e) {
					throw new CLIStatusException("read_dsl_file_failed", templatesPath, e.getMessage());
				}
			} else { 
				// templatesFileOrDir is a groovy file
				if (!templatesFolderName.endsWith(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX)) {
					throw new CLIStatusException("illegal_template_file_name", templatesFolderName);
				}
				File parentFile = templatesFileOrDir.getParentFile();
				File[] actualTemplatesDslFiles = 
						DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, parentFile);
				if (actualTemplatesDslFiles.length > 1) {
					throw new CLIStatusException("too_many_template_files", Arrays.toString(actualTemplatesDslFiles));
				} 
				try {
					numTemplatesInFolder = reader.readCloudTemplatesFromFile(templatesFileOrDir).size();
				} catch (DSLException e) {
					throw new CLIStatusException("read_dsl_file_failed", templatesPath, e.getMessage());
				}
				if (numTemplatesInFolder == 0) { 
					throw new CLIStatusException("no_template_files", templatesPath);
				}
				zipFile = Packager.createZipFile("templates", parentFile);
			}
		} else {
			// templatesFileOrDir is a directory
			try {
				numTemplatesInFolder = reader.readCloudTemplatesFromDirectory(templatesFileOrDir).size();
				if (numTemplatesInFolder == 0) {
					throw new CLIStatusException("no_template_files", templatesPath);
				}
			} catch (DSLException e) {
				throw new CLIStatusException("read_dsl_file_failed", templatesPath, e.getMessage());
			}
			zipFile = Packager.createZipFile("templates", templatesFileOrDir);
		}
		// add the templates to the cloud
		logger.info("Adding " + numTemplatesInFolder + " templates to cloud.");
		List<String> addedTemplates;
		try {
			addedTemplates = adminFacade.addTemplates(zipFile);
		} catch (CLIStatusException e) {
			String reasonCode = e.getReasonCode();
			if (reasonCode.equals(CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName()) || 
					reasonCode.equals(CloudifyErrorMessages.PARTLY_FAILED_TO_ADD_TEMPLATES.getName())) {
				throw new CLIStatusException(reasonCode, convertArgsToIndentJason(e.getArgs()));
			} else if (reasonCode.equals("failed_to_add_all_templates")) {
				if (e.getArgs().length > 0) {
					throw new CLIStatusException(reasonCode, getIndentMap((Map<String, Object>) e.getArgs()[0]));
				}
			}
			throw e;
		}
		return getFormattedMessage("templates_added_successfully", Color.GREEN) 
				+ getFormatedAddedTemplateNamesList(addedTemplates);
	}

	private static Object[] convertArgsToIndentJason(final Object[] args) {
		String[] newArgs = new String[args.length];
		if (newArgs.length < 2) {
			return args;
		}
		Map<String, Map<String, String>> failedToAddTemplates = (Map<String, Map<String, String>>) args[0];
		StringBuilder failedToAddTemplatesStr = new StringBuilder();
		if (failedToAddTemplates.isEmpty()) {
			failedToAddTemplatesStr.append("{ }");
		} else {
			failedToAddTemplatesStr.append(CloudifyConstants.NEW_LINE)
			.append("{")
			.append(CloudifyConstants.NEW_LINE);
			for (Entry<String, Map<String, String>> entry : failedToAddTemplates.entrySet()) {
				Map<String, String> failedToAddTemplatesErrDesc = entry.getValue();
				failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
				.append(entry.getKey())
				.append(":")
				.append(CloudifyConstants.NEW_LINE)
				.append(CloudifyConstants.TAB_CHAR)
				.append("{")
				.append(CloudifyConstants.NEW_LINE);
				for (Entry<String, String> templateErrDesc : failedToAddTemplatesErrDesc.entrySet()) {
					failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
					.append(CloudifyConstants.TAB_CHAR)
					.append(templateErrDesc.getKey())
					.append(" - ")
					.append(templateErrDesc.getValue())
					.append(CloudifyConstants.NEW_LINE);
				}
				failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
				.append("}")
				.append(CloudifyConstants.NEW_LINE);
			}
			failedToAddTemplatesStr.append("}");
		}
		newArgs[0] = failedToAddTemplatesStr.toString();
		
		newArgs[1] = getIndentMap((Map<String, Object>) args[1]);

		return newArgs;
	}

	private static String getIndentMap(final Map<String, Object> map) {
		StringBuilder successfullyAddedTemplatesStr = new StringBuilder();
		if (map.isEmpty()) {
			successfullyAddedTemplatesStr.append("{ }");
		} else {
			successfullyAddedTemplatesStr.append(CloudifyConstants.NEW_LINE)
			.append("{")
			.append(CloudifyConstants.NEW_LINE);
			for (Entry<String, Object> entry : map.entrySet()) {
				successfullyAddedTemplatesStr.append(CloudifyConstants.TAB_CHAR)
				.append(entry.getKey())
				.append(": ")
				.append(entry.getValue())
				.append(CloudifyConstants.NEW_LINE);
			}
			successfullyAddedTemplatesStr.append("}");
		}
		return successfullyAddedTemplatesStr.toString();
	}
	
	private static Object getFormatedAddedTemplateNamesList(final List<String> templates) {
		StringBuilder sb = new StringBuilder(CloudifyConstants.NEW_LINE)
            .append("Added ").append(templates.size()).append(" templates:");

		for (String templateName : templates) {
			sb.append(CloudifyConstants.NEW_LINE)
			.append(CloudifyConstants.TAB_CHAR)
			.append(templateName);
		}
		return sb;
	}

}
