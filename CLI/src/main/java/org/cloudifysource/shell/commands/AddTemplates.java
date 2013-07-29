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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.utilitydomain.data.ComputeTemplateHolder;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.jansi.Ansi.Color;

/**
 * Adds templates to be included in the cloud's templates list. Reads the templates from the (groovy) templates-file.
 * 
 * Required arguments: templates-file-or-folder - Path to a single groovy file (one template to add) or to a folder
 * (zipped or not) contains one or more groovy files each groovy file has the form of "*-template.groovy" and declare
 * one template to add.
 * 
 * Command syntax: add-templates templates-file-or-folder
 * 
 * @author yael
 * 
 * @since 2.3.0
 * 
 */
@Command(scope = "cloudify", name = "add-templates", description = "Adds templates to the cloud")
public class AddTemplates extends AdminAwareCommand implements NewRestClientCommand {

	@Argument(required = true, name = "templates-file-or-folder",
			description = "A template file or a templates folder that can contain several template files.")
	private File templatesFileOrDir;

	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	@Override
	protected Object doExecute() throws Exception {

		final String templatesPath = templatesFileOrDir.getAbsolutePath();
		validateTemplateFile(templatesPath);
		final boolean isZipFile = isZipFile(templatesFileOrDir);
		final File templatesFolder = getTemplatesFolder(isZipFile);
		final List<ComputeTemplateHolder> expectedTemplates = 
				new ComputeTemplatesReader().readCloudTemplatesFromDirectory(templatesFolder);
		File zipFile = templatesFileOrDir;
		if (!isZipFile) {
			zipFile = Packager.createZipFile("templates", templatesFolder);
		}
		// add the templates to the cloud
		logger.info("Adding " + expectedTemplates.size() + " templates to cloud.");
		List<String> addedTemplates;
		try {
			addedTemplates = adminFacade.addTemplates(zipFile);
		} catch (final CLIStatusException e) {
			final String reasonCode = e.getReasonCode();
			if (reasonCode.equals(CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName())
					|| reasonCode.equals(CloudifyErrorMessages.PARTLY_FAILED_TO_ADD_TEMPLATES.getName())) {
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

	private File getTemplatesFolder(final boolean isZipFile) throws IOException, CLIStatusException {
		final String templatesFolderName = templatesFileOrDir.getName();
		if (templatesFileOrDir.isFile()) {
			if (isZipFile) {
				return new ComputeTemplatesReader().unzipCloudTemplatesFolder(templatesFileOrDir);
			}
			// templatesFileOrDir is a groovy file
			if (!templatesFolderName.endsWith(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX)) {
				throw new CLIStatusException("illegal_template_file_name", templatesFolderName);
			}
			final File parentFile = templatesFileOrDir.getParentFile();
			final File[] actualTemplatesDslFiles =
					DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, parentFile);
			if (actualTemplatesDslFiles.length > 1) {
				throw new CLIStatusException("too_many_template_files", Arrays.toString(actualTemplatesDslFiles));
			}
			return parentFile;
		}
		// templatesFileOrDir is a directory
		return templatesFileOrDir;
	}

	private boolean isZipFile(final File templatesFileOrDir) {
		final String templatesFolderName = templatesFileOrDir.getName();
		return templatesFolderName.endsWith(".zip") || templatesFolderName.endsWith(".jar");
	}

	private static Object[] convertArgsToIndentJason(final Object[] args) {
		final String[] newArgs = new String[args.length];
		if (newArgs.length < 2) {
			return args;
		}
		final Map<String, Map<String, String>> failedToAddTemplates = (Map<String, Map<String, String>>) args[0];
		final StringBuilder failedToAddTemplatesStr = new StringBuilder();
		if (failedToAddTemplates.isEmpty()) {
			failedToAddTemplatesStr.append("{ }");
		} else {
			failedToAddTemplatesStr.append(CloudifyConstants.NEW_LINE)
					.append("{")
					.append(CloudifyConstants.NEW_LINE);
			for (final Entry<String, Map<String, String>> entry : failedToAddTemplates.entrySet()) {
				final Map<String, String> failedToAddTemplatesErrDesc = entry.getValue();
				failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
						.append(entry.getKey())
						.append(":")
						.append(CloudifyConstants.NEW_LINE)
						.append(CloudifyConstants.TAB_CHAR)
						.append("{")
						.append(CloudifyConstants.NEW_LINE);
				for (final Entry<String, String> templateErrDesc : failedToAddTemplatesErrDesc.entrySet()) {
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
		final StringBuilder successfullyAddedTemplatesStr = new StringBuilder();
		if (map.isEmpty()) {
			successfullyAddedTemplatesStr.append("{ }");
		} else {
			successfullyAddedTemplatesStr.append(CloudifyConstants.NEW_LINE)
					.append("{")
					.append(CloudifyConstants.NEW_LINE);
			for (final Entry<String, Object> entry : map.entrySet()) {
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
		final StringBuilder sb = new StringBuilder(CloudifyConstants.NEW_LINE)
				.append("Added ").append(templates.size()).append(" templates:");

		for (final String templateName : templates) {
			sb.append(CloudifyConstants.NEW_LINE)
					.append(CloudifyConstants.TAB_CHAR)
					.append(templateName);
		}
		return sb;
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {
		final RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();
		final String templatesPath = templatesFileOrDir.getAbsolutePath();
		validateTemplateFile(templatesPath);
		final boolean isZipFile = isZipFile(templatesFileOrDir);
		final File templatesFolder = getTemplatesFolder(isZipFile);
		final List<ComputeTemplateHolder> expectedTemplates = 
				new ComputeTemplatesReader().readCloudTemplatesFromDirectory(templatesFolder);
		File zipFile = templatesFileOrDir;
		if (!isZipFile) {
			zipFile = Packager.createZipFile("templates", templatesFolder);
		}
		// add the templates to the cloud
		logger.info("Adding " + expectedTemplates.size() + " templates to cloud:"
				+ printExpectedTemplates(expectedTemplates));
		final String uploadKey = ShellUtils.uploadToRepo(newRestClient, zipFile, displayer);
		final AddTemplatesRequest request = new AddTemplatesRequest();
		request.setUploadKey(uploadKey);
		try {
			final AddTemplatesResponse response = newRestClient.addTemplates(request);
			String result;
			final Map<String, Map<String, String>> failedToAddTempaltes = response.getFailedToAddTempaltes();
			final List<String> successfullyAddedTempaltes = response.getSuccessfullyAddedTempaltes();
			if (failedToAddTempaltes.isEmpty()) {
				result = getFormattedMessage("templates_added_successfully", Color.GREEN)
						+ getFormatedAddedTemplateNamesList(successfullyAddedTempaltes);
			} else {
				// partial failure
				result = getPartialFailureMessage(successfullyAddedTempaltes, failedToAddTempaltes);
			}
			return result;
		} catch (RestClientException e) {
			// failure
			return getFailureMessage(expectedTemplates);
		}
	}

	private String getFailureMessage(List<ComputeTemplateHolder> expectedTemplates) {
		List<String> expectedTempalteNames = new LinkedList<String>();
		for (ComputeTemplateHolder templateHolder : expectedTemplates) {
			expectedTempalteNames.add(templateHolder.getName());
		}
		return getFormattedMessage("failed_to_add_templates", Color.RED, expectedTempalteNames);
	}

	private String printExpectedTemplates(final List<ComputeTemplateHolder> expectedTemplates) {
		final StringBuilder sb = new StringBuilder();
		for (final ComputeTemplateHolder computeTemplateHolder : expectedTemplates) {
			sb.append(CloudifyConstants.NEW_LINE);
			sb.append(computeTemplateHolder.getName());
		}
		return sb.toString();
	}

	private String getPartialFailureMessage(final List<String> successfullyAddedTempaltes,
			final Map<String, Map<String, String>> failedToAddTempaltes) {
		final StringBuilder sb = new StringBuilder("Partial Failure:" + CloudifyConstants.NEW_LINE);
		if (!successfullyAddedTempaltes.isEmpty()) {
			sb.append("The following templates were added to all REST instances: " + successfullyAddedTempaltes);
			sb.append(CloudifyConstants.NEW_LINE);
		}
		sb.append("The following templates failed to be added to one or more REST instances:");
		sb.append(CloudifyConstants.NEW_LINE);
		try {
			sb.append(new ObjectMapper().writeValueAsString(failedToAddTempaltes));
		} catch (final Exception e) {
			logger.log(Level.WARNING, "failed to write failure map as String, use toString instead.");
			e.printStackTrace();
			sb.append(failedToAddTempaltes.toString());
		}
		return sb.toString();
	}

	private void validateTemplateFile(final String templatesPath)
			throws CLIStatusException {
		logger.info("Validating template folder and files: " + templatesPath);
		if (!templatesFileOrDir.exists()) {
			throw new CLIStatusException("templates_file_not_found", templatesPath);
		}
	}

}
