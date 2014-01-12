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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.domain.ComputeTemplateHolder;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplateResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesStatus;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
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
			return getFormattedMessage("templates_added_successfully", Color.GREEN, 
					getSuccessfulMessage(response));

		} catch (AddTemplatesException e) {
			// failure or partial failure
			return getFailureMessage(e.getAddTemplatesResponse());
		}
	}

	private static String getIndentJson(final String body) throws IOException {
		if (StringUtils.isBlank(body)) {
			return null;
		}

		StringWriter out = new StringWriter();
		JsonParser parser = null;
		JsonGenerator gen = null;
		try {
			JsonFactory fac = new JsonFactory();

			parser = fac.createJsonParser(new StringReader(body));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(parser);
			// Create pretty printer:
			gen = fac.createJsonGenerator(out);
			gen.useDefaultPrettyPrinter();
			// Write:
			mapper.writeTree(gen, node);

			gen.close();
			parser.close();

			return out.toString();

		} finally {
			out.close();
			if (gen != null) {
				gen.close();
			}
			if (parser != null) {
				parser.close();
			}
		}
	}

	private static Object getFormatedAddedTemplateNamesList(final List<String> templates) {
		int size = templates.size();
		final StringBuilder sb = new StringBuilder(CloudifyConstants.NEW_LINE)
				.append("The ").append(size).append(" template" + (size == 1 ? "" : "s") + " added:");

		for (final String templateName : templates) {
			sb.append(CloudifyConstants.NEW_LINE)
					.append(CloudifyConstants.TAB_CHAR)
					.append(templateName);
		}
		return sb;
	}

	private String getFailureMessage(final AddTemplatesResponse addTemplatesResponse) 
			throws IOException {
		List<String> instances = addTemplatesResponse.getInstances();
		int size = instances.size();
		StringBuilder sb = new StringBuilder("Add templates to " 
				+ size + " REST instance" + (size == 1 ? " " : "s ") + instances + " resulted with ");
		if (AddTemplatesStatus.PARTIAL_FAILURE.equals(addTemplatesResponse.getStatus())) {
			sb.append("partial failure (at least one template failed to be added to at least one REST instance):");
		} else {
			sb.append("failure (all templates failed to be added to all REST instances):");
		}
		sb.append(CloudifyConstants.NEW_LINE);
		
		Map<String, AddTemplateResponse> templates = addTemplatesResponse.getTemplates();
		Map<String, Map<String, String>> resultMap = new HashMap<String, Map<String, String>>(); 
		ObjectMapper objectMapper = new ObjectMapper();
		for (Entry<String, AddTemplateResponse> entry : templates.entrySet()) {
			AddTemplateResponse addTemplateResponse = entry.getValue();
			Map<String, String> templateResultMap = new HashMap<String, String>();
			templateResultMap.put("failed to add to", 
					objectMapper.writeValueAsString(addTemplateResponse.getFailedToAddHosts()));
			templateResultMap.put("successfully added to", 
					objectMapper.writeValueAsString(addTemplateResponse.getSuccessfullyAddedHosts()));
			resultMap.put(entry.getKey(), templateResultMap);
		}
		sb.append(getIndentJson(objectMapper.writeValueAsString(resultMap)));
		return sb.toString().replaceAll("\"", "").replaceAll("\\\\", "");
	}

	private String getSuccessfulMessage(final AddTemplatesResponse response) {
		StringBuilder sb = new StringBuilder();
		sb.append(CloudifyConstants.NEW_LINE);
		sb.append("Templates were added to all REST instances: ");
		sb.append(response.getInstances());
		Map<String, AddTemplateResponse> templates = response.getTemplates();
		List<String> templateNames = new LinkedList<String>();
		for (String templateName : templates.keySet()) {
			templateNames.add(templateName);
		}
		sb.append(getFormatedAddedTemplateNamesList(templateNames));
		return sb.toString();
	}

	private String printExpectedTemplates(final List<ComputeTemplateHolder> expectedTemplates) {
		final StringBuilder sb = new StringBuilder();
		for (final ComputeTemplateHolder computeTemplateHolder : expectedTemplates) {
			sb.append(CloudifyConstants.NEW_LINE);
			sb.append(computeTemplateHolder.getName());
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
