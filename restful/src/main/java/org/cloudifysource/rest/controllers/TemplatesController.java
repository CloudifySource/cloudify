/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.controllers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.ComputeTemplateHolder;
import org.cloudifysource.dsl.internal.ComputeTemplatesReader;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.rest.request.AddTemplatesInternalRequest;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesInternalResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.GetTemplateResponse;
import org.cloudifysource.dsl.rest.response.ListTemplatesResponse;
import org.cloudifysource.dsl.rest.response.RemoveTemplatesResponse;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.repo.UploadRepo;
import org.cloudifysource.rest.util.RestUtils;
import org.cloudifysource.rest.validators.AddTemplatesValidationContext;
import org.cloudifysource.rest.validators.AddTemplatesValidator;
import org.cloudifysource.rest.validators.TemplatesValidationContext;
import org.cloudifysource.rest.validators.TemplatesValidator;
import org.cloudifysource.restDoclet.annotations.InternalMethod;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.security.CustomPermissionEvaluator;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.j_spaces.kernel.PlatformVersion;

/**
 * @author yael
 * @since 2.7.0
 * 
 */
@Controller
@RequestMapping(value = "/{version}/templates")
public class TemplatesController extends BaseRestController {
	private static final Logger logger = Logger.getLogger(TemplatesController.class.getName());
	
	@Autowired
	private RestConfiguration restConfig;
	@Autowired
	private UploadRepo repo;
	@Autowired
	private final AddTemplatesValidator[] addTemplatesValidators = new AddTemplatesValidator[0];
	@Autowired
	private final TemplatesValidator[] templatesValidators = new TemplatesValidator[0];
	private Cloud cloud;
	private Admin admin;
	private CustomPermissionEvaluator permissionEvaluator;
	private File cloudConfigurationDir;

	/**
	 * Initialization.
	 */
	@PostConstruct
	public void init() {
		cloud = restConfig.getCloud();
		admin = restConfig.getAdmin();
		permissionEvaluator = restConfig.getPermissionEvaluator();
		cloudConfigurationDir = restConfig.getCloudConfigurationDir();
	}

	/**
	 * Add templates from templates folder to the cloud. Returns a response in case of success or partial failure.
	 * 
	 * @param request
	 *            {@link AddTemplatesRequest}
	 * @return {@link AddTemplatesResponse}
	 * @throws RestErrorException
	 *             if failed to add all templates to all REST instances.
	 * @throws IOException
	 *             If failed to unzip templates folder.
	 * @throws DSLException
	 *             If failed to read the templates from templates folder.
	 */
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(method = RequestMethod.POST)
	public AddTemplatesResponse addTemplates(@RequestBody final AddTemplatesRequest request)
			throws RestErrorException, IOException, DSLException {
		logger.log(Level.INFO, "[addTemplates] - starting add templates.");
		// validate
		validateAddTemplates(request);
		File templatesZippedFolder = null;
		try {
			// get templates folder
			final String uploadKey = request.getUploadKey();
			templatesZippedFolder = repo.get(uploadKey);
			if (templatesZippedFolder == null) {
				throw new RestErrorException(CloudifyMessageKeys.WRONG_TEMPLATES_UPLOAD_KEY.getName(), uploadKey);
			}
			final AddTemplatesInternalRequest internalRequest = createInternalRequest(request, templatesZippedFolder);
			// add the templates to all REST instances
			return addTemplatesToEachRestInstance(internalRequest);
		} finally {
			if (templatesZippedFolder != null) {
				FileUtils.deleteQuietly(templatesZippedFolder);
			}
		}

	}

	/**
	 * Get the cloud's templates.
	 * 
	 * @return {@link ListTemplatesResponse} containing the cloud's templates.
	 * @throws RestErrorException
	 *             If cloud is a local cloud.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS', 'ROLE_APPMANAGERS')")
	public ListTemplatesResponse listTemplates()
			throws RestErrorException {
		validateTemplateOperation("list-templates");
		final ListTemplatesResponse response = new ListTemplatesResponse();
		final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
		response.setTemplates(templates);
		return response;
	}

	/**
	 * Get template from the cloud.
	 * 
	 * @param templateName
	 *            The name of the template to get.
	 * @return a map containing the template and a success status if succeeded, else returns an error status.
	 * @throws RestErrorException
	 *             if the cloud is a local cloud or the template doesn't exist.
	 */
	@RequestMapping(value = "{templateName}", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS', 'ROLE_APPMANAGERS')")
	public GetTemplateResponse getTemplate(@PathVariable final String templateName)
			throws RestErrorException {

		validateTemplateOperation("get-template");

		// get template from cloud
		final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(templateName);

		if (cloudTemplate == null) {
			logger.log(Level.WARNING, "[getTemplate] - template [" + templateName
					+ "] not found. cloud templates list: " + cloud.getCloudCompute().getTemplates());
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_NOT_EXIST.getName(), templateName);
		}
		final GetTemplateResponse response = new GetTemplateResponse();
		response.setTemplate(cloudTemplate);
		return response;
	}

	private AddTemplatesInternalRequest createInternalRequest(final AddTemplatesRequest request,
			final File templatesZippedFolder)
			throws DSLException, IOException {

		final AddTemplatesInternalRequest internalRequest = new AddTemplatesInternalRequest();
		// upload key
		internalRequest.setUploadKey(request.getUploadKey());
		// cloud templates
		final File unzippedFolder = new ComputeTemplatesReader().unzipCloudTemplatesFolder(templatesZippedFolder);
		try {
			final List<ComputeTemplateHolder> cloudTemplatesHolders =
					new ComputeTemplatesReader().readCloudTemplatesFromDirectory(unzippedFolder);
			internalRequest.setCloudTemplates(cloudTemplatesHolders);
			// expected templates
			final List<String> expectedAddedTemplates = new LinkedList<String>();
			for (final ComputeTemplateHolder templateHolder : cloudTemplatesHolders) {
				// rename template file to <templateName>-template.groovy if needed
				// rename the properties and overrides files as well.
				renameTemplateFilesIfNeeded(unzippedFolder, templateHolder);
				expectedAddedTemplates.add(templateHolder.getName());
			}
			internalRequest.setExpectedTemplates(expectedAddedTemplates);

			return internalRequest;

		} finally {
			if (unzippedFolder != null) {
				FileUtils.deleteQuietly(unzippedFolder);
			}
		}
	}

	/**
	 * For each puInstance - send the invoke an add templates request.
	 * 
	 * @param templatesFolder
	 *            .
	 * @param expectedTemplates
	 *            The expected templates to add.
	 * @param addedTemplatesByHost
	 *            a map updates by this method to specify the failed to add templates for each instance.
	 * @param failedToAddTemplatesByHost
	 *            a map updates by this method to specify the failed to add templates for each instance.
	 * @throws RestErrorException
	 *             If failed to add all templates (no template was added).
	 */
	private AddTemplatesResponse addTemplatesToEachRestInstance(final AddTemplatesInternalRequest request)
			throws RestErrorException {

		final Map<String, Map<String, String>> failedToAddTempaltes =
				new HashMap<String, Map<String, String>>();
		final List<String> successfullyAddedTemplates = new LinkedList<String>();
		successfullyAddedTemplates.addAll(request.getExpectedTemplates());

		int failedHostCount = 0;

		// get the instances
		final ProcessingUnitInstance[] instances = admin.getProcessingUnits().
				waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS).getInstances();
		// execute add-template on each rest instance
		logger.log(Level.INFO, "[addTemplatesToRestInstances] - sending add templates request to "
				+ instances.length + " instances.");
		for (final ProcessingUnitInstance puInstance : instances) {
			final String port = Integer.toString(puInstance.getJeeDetails().getPort());
			String hostAddress = puInstance.getMachine().getHostAddress();
			logger.log(Level.INFO, "[addTemplatesToRestInstances] - execute add-templates to " + hostAddress);
			final AddTemplatesInternalResponse instanceResponse = 
					executeAddTemplateOnInstance(hostAddress, port, request);
			final Map<String, String> failedToAddTempaltesToHost = instanceResponse.getFailedToAddTempaltesAndReasons();
			if (failedToAddTempaltesToHost != null && !failedToAddTempaltesToHost.isEmpty()) {
				for (final Entry<String, String> entry : failedToAddTempaltesToHost.entrySet()) {
					final String templateName = entry.getKey();
					final String reason = entry.getValue();
					// remove each failed template from the successfully added list.
					successfullyAddedTemplates.remove(templateName);
					// add this PU and reason to the template's map in failure templates map.
					Map<String, String> templateFailedPUs = failedToAddTempaltes.get(templateName);
					if (templateFailedPUs == null) {
						templateFailedPUs = new HashMap<String, String>();
					}
					templateFailedPUs.put(hostAddress, reason);
					failedToAddTempaltes.put(templateName, templateFailedPUs);
				}
				final List<String> addedTempaltes = instanceResponse.getAddedTempaltes();
				logger.log(Level.WARNING, "[addTemplatesToRestInstances] - failed to add templates to host ["
						+ hostAddress + "]: " + failedToAddTempaltesToHost);
				if (addedTempaltes == null || addedTempaltes.isEmpty()) {
					// all expected templates failed to be added
					failedHostCount++;
				} else {
					// partial success
					logger.log(Level.WARNING, "[addTemplatesToRestInstances] - successfully added templates: "
							+ addedTempaltes);
				}
			} else {
				// all expected templates were added
				logger.log(Level.INFO,
						"[addTemplatesToRestInstances] - successfully added templates to host [" + hostAddress + "].");
			}
		}

		// check if all PUs failed.
		if (instances.length == failedHostCount) {
			logger.log(Level.WARNING,
					"[sendAddTemplatesToRestInstances] - Failed to add templates: " + failedToAddTempaltes);
			throw new RestErrorException(CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName(),
					failedToAddTempaltes.toString());
		}
		// no failure => there is at least one PU that successfully added at least one template.
		// create and return the response.
		final AddTemplatesResponse response = new AddTemplatesResponse();
		response.setFailedToAddTempaltes(failedToAddTempaltes);
		response.setSuccessfullyAddedTempaltes(successfullyAddedTemplates);
		return response;

	}

	private boolean isLocalHost(final String hostAddress) {
		String localhost;
		// TODO : refine next log
		logger.log(Level.INFO, "[isAddTemplateToSelf] - checking if " + hostAddress
				+ " is local host.");
		try {
			localhost = InetAddress.getLocalHost().getHostAddress();
		} catch (final UnknownHostException e) {
			logger.log(Level.WARNING, "[isAddTemplateToSelf] - failed to get local host address, returning false."
					+ " Error message: " + e.getMessage(), e);
			return false;
		}
		final boolean equals = localhost.equals(hostAddress);
		if (!equals) {
			// TODO : refine next log
			logger.log(Level.INFO, "[isAddTemplateToSelf] - the host to check [" + hostAddress
					+ "] is not the local host [" + localhost + "].");
		}
		return equals;
	}

	/**
	 * Invoke add templates on the given instance.
	 * 
	 * @param puInstance
	 * @param request
	 * @param host 
	 * @return AddTemplatesInternalResponse
	 */
	private AddTemplatesInternalResponse executeAddTemplateOnInstance(
			final String host,
			final String port,
			final AddTemplatesInternalRequest request) {
		AddTemplatesInternalResponse instanceResponse;
		try {
			// invoke add-templates command on each REST instance.
			RestClient createRestClient = createRestClient(host, port);
			instanceResponse = createRestClient.addTemplatesInternal(request);
		} catch (final RestClientException e) {
			// the request failed => all expected templates failed to be added
			// create a response that contains all expected templates in a failure map.
			logger.log(Level.WARNING, "[executeAddTemplateOnInstance] - Failed to execute http request to "
					+ host  + ". Error message: " + e.getMessageFormattedText());
			final Map<String, String> failedMap = new HashMap<String, String>();
			for (final String expectedTemplate : request.getExpectedTemplates()) {
				failedMap.put(expectedTemplate, "http request failed [" + e.getMessageFormattedText() + "]");
			}
			instanceResponse = new AddTemplatesInternalResponse();
			instanceResponse.setFailedToAddTempaltesAndReasons(failedMap);
			return instanceResponse;
		}
		final List<String> addedTempaltes = instanceResponse.getAddedTempaltes();
		// TODO : refine next log
		logger.log(Level.INFO, "[executeAddTemplateOnInstance] - added "
				+ addedTempaltes.size() + " templates: " + addedTempaltes);
		final Map<String, String> failedToAddTempaltesAndReasons =
				instanceResponse.getFailedToAddTempaltesAndReasons();
		final List<String> failedList = new ArrayList<String>(failedToAddTempaltesAndReasons.keySet());
		// TODO : refine next log
		logger.log(Level.INFO, "[executeAddTemplateOnInstance] - failed to add "
				+ failedList.size() + " templates: " + failedList);
		// addedTempaltes and failedList suppose to contain all templates from expectedTemplates.
		final List<?> union = ListUtils.union(addedTempaltes, failedList);
		// TODO : remove next log
		logger.log(Level.INFO, "[executeAddTemplateOnInstance] - union of  addedTempaltes and failedList: " + union);
		final List<?> subtract = ListUtils.subtract(request.getExpectedTemplates(), union);
		// TODO : remove next log
		logger.log(Level.INFO, "[executeAddTemplateOnInstance] - subtract union from expectedTemplates ["
				+ request.getExpectedTemplates() + "]: " + subtract);
		if (!subtract.isEmpty()) {
			// add all missing templates to the failure map.
			for (final Object templateName : subtract) {
				failedToAddTempaltesAndReasons.put((String) templateName,
						"expected template missing (not found in failure list)");
			}
			instanceResponse.setFailedToAddTempaltesAndReasons(failedToAddTempaltesAndReasons);
		}
		return instanceResponse;
	}

	/**
	 * Add template files to the cloud configuration directory and to the cloud object. This method supposed to be
	 * invoked by the MNG on all REST instances.
	 * 
	 * @param request
	 *            The request.
	 * @return {@link AddTemplatesInternalResponse}
	 * @throws IOException
	 *             in case of reading error.
	 */
	@InternalMethod
	@RequestMapping(value = "internal", method = RequestMethod.POST)
	private AddTemplatesInternalResponse
			addTemplatesInternal(
					@RequestBody final AddTemplatesInternalRequest request)
					throws IOException {

		final ComputeTemplatesReader reader = new ComputeTemplatesReader();
		final File templatesFolder = repo.get(request.getUploadKey());
		final File unzippedTemplatesFolder = reader.unzipCloudTemplatesFolder(templatesFolder);

		return addTemplatesInternal(unzippedTemplatesFolder, request.getCloudTemplates());
	}

	private AddTemplatesInternalResponse
			addTemplatesInternal(final File unzippedTemplatesFolder, final List<ComputeTemplateHolder> templates) {
		try {
			logger.log(Level.INFO, "[addTemplatesInternal] - adding templates from templates folder: "
					+ unzippedTemplatesFolder.getAbsolutePath());
			// add templates to the cloud and return the added templates.
			return addTemplatesToCloud(unzippedTemplatesFolder, templates);
		} finally {
			FileUtils.deleteQuietly(unzippedTemplatesFolder);
		}
	}

	/**
	 * Adds templates to cloud's templates. Adds templates' files to cloud configuration directory.
	 * 
	 * @param templatesFolder
	 * @return {@link AddTemplatesInternalResponse}
	 */
	private AddTemplatesInternalResponse addTemplatesToCloud(final File templatesFolder,
			final List<ComputeTemplateHolder> templatesHolders) {

		// TODO : refine next log
		logger.log(Level.INFO, "[addTemplatesToCloud] - Adding " + templatesHolders.size() + " templates to cloud.");
		// adds the templates to the cloud's templates list, deletes the failed to added templates from the folder.
		final AddTemplatesInternalResponse addTemplatesToCloudListresponse =
				addTemplatesToCloudList(templatesFolder, templatesHolders);
		List<String> addedTemplates = addTemplatesToCloudListresponse.getAddedTempaltes();
		final Map<String, String> failedToAddTemplates =
				addTemplatesToCloudListresponse.getFailedToAddTempaltesAndReasons();
		// if no templates were added, throw an exception
		if (addedTemplates.isEmpty()) {
			logger.log(Level.WARNING, "[addTemplatesToCloud] - Failed to add templates from "
					+ templatesFolder.getAbsolutePath());
		} else {
			// at least one template was added, copy files from template folder to a new folder.
			logger.log(Level.INFO,
					"[addTemplatesToCloud] - Coping templates files from " + templatesFolder.getAbsolutePath()
							+ " to a new folder under " + cloudConfigurationDir.getAbsolutePath());
			try {
				final File localTemplatesDir = copyTemplateFilesToCloudConfigDir(templatesFolder);
				// TODO : refine next log
				logger.log(Level.INFO,
						"[addTemplatesToCloud] - The templates files copied to " + localTemplatesDir.getAbsolutePath());
				updateCloudTemplatesUploadPath(addedTemplates, localTemplatesDir);
			} catch (final IOException e) {
				// failed to copy files - remove all added templates from cloud and them to the failed map.
				logger.log(Level.WARNING,
						"[addTemplatesToCloud] - Failed to copy templates files, error: " + e.getMessage(), e);
				for (final String templateName : addedTemplates) {
					cloud.getCloudCompute().getTemplates().remove(templateName);
					failedToAddTemplates.put(templateName, "failed to copy templates files");
				}
				// added templates should not include templates.
				addedTemplates = new LinkedList<String>();
			}
		}
		if (!failedToAddTemplates.isEmpty()) {
			logger.log(Level.INFO, "[addTemplatesToCloud] - Failed to add the following templates: "
					+ failedToAddTemplates.toString());
		}
		// create and return the result.
		final AddTemplatesInternalResponse response = new AddTemplatesInternalResponse();
		response.setAddedTempaltes(addedTemplates);
		response.setFailedToAddTempaltesAndReasons(failedToAddTemplates);
		return response;
	}

	/**
	 * Updates the upload local path in all added cloud templates.
	 * 
	 * @param addedTemplates
	 *            the added templates.
	 * @param localTemplatesDir
	 *            the directory where the upload directory expected to be found.
	 */
	private void updateCloudTemplatesUploadPath(final List<String> addedTemplates, final File localTemplatesDir) {
		for (final String templateName : addedTemplates) {
			final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
			final String localUploadPath =
					new File(localTemplatesDir, cloudTemplate.getLocalDirectory()).getAbsolutePath();
			cloudTemplate.setAbsoluteUploadDir(localUploadPath);
		}
	}

	/**
	 * Scans the cloudTemplatesHolders list and adds each template that doesn't already exist. Rename template's file if
	 * needed (if its prefix is not the template's name).
	 * 
	 * @param templatesFolder
	 *            the folder contains templates files.
	 * @param cloudTemplates
	 *            the list of cloud templates.
	 * @param addedTemplates
	 *            a list for this method to update with all the added templates.
	 * @param failedToAddTemplates
	 *            a list for this method to update with all the failed to add templates.
	 */
	private AddTemplatesInternalResponse addTemplatesToCloudList(
			final File templatesFolder, final List<ComputeTemplateHolder> cloudTemplates) {
		final List<String> addedTemplates = new LinkedList<String>();
		final Map<String, String> failedToAddTemplates = new HashMap<String, String>();
		// TODO : refine next log
		logger.log(Level.INFO, "[addTemplatesToCloudList] - adding " + cloudTemplates.size()
				+ " templates to cloud's list.");
		for (final ComputeTemplateHolder holder : cloudTemplates) {
			final String templateName = holder.getName();
			// TODO : remove next log
			logger.log(Level.INFO, "[addTemplatesToCloudList] - adding " + templateName + " to cloud's list.");
			final String originalTemplateFileName = holder.getTemplateFileName();
			// check if template already exist
			final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
			// TODO : remove next log
			logger.log(Level.INFO, "[addTemplatesToCloudList] - cloud's templates: " + templates);
			if (templates.containsKey(templateName)) {
				// template already exists
				logger.log(Level.WARNING, "[addTemplatesToCloudList] - Template already exists: " + templateName);
				failedToAddTemplates.put(templateName, "template already exists");
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			// add template to cloud templates list
			final ComputeTemplate cloudTemplate = holder.getCloudTemplate();
			templates.put(templateName, cloudTemplate);
			addedTemplates.add(templateName);
		}
		final AddTemplatesInternalResponse response = new AddTemplatesInternalResponse();
		response.setAddedTempaltes(addedTemplates);
		response.setFailedToAddTempaltesAndReasons(failedToAddTemplates);
		return response;
	}

	/**
	 * Copies all the files from templatesFolder to a new directory under cloud configuration directory.
	 * 
	 * @param templatesDirToCopy
	 *            the directory contains all the files to copy.
	 * @throws IOException
	 *             If failed to copy files.
	 */
	private File copyTemplateFilesToCloudConfigDir(final File templatesDirToCopy)
			throws IOException {
		final File templatesDirParent = getTemplatesFolder();
		// create new templates folder - increment folder number until no folder with that name exist.
		String folderName = CloudifyConstants.TEMPLATE_FOLDER_PREFIX
				+ restConfig.getLastTemplateFileNum().incrementAndGet();
		File copiedtemplatesFolder = new File(templatesDirParent, folderName);
		while (copiedtemplatesFolder.exists()) {
			folderName = CloudifyConstants.TEMPLATE_FOLDER_PREFIX
					+ restConfig.getLastTemplateFileNum().incrementAndGet();
			copiedtemplatesFolder = new File(templatesDirParent, folderName);
		}
		copiedtemplatesFolder.mkdir();
		try {
			FileUtils.copyDirectory(templatesDirToCopy, copiedtemplatesFolder);
			return copiedtemplatesFolder;
		} catch (final IOException e) {
			FileUtils.deleteDirectory(copiedtemplatesFolder);
			restConfig.getLastTemplateFileNum().decrementAndGet();
			throw e;
		}
	}

	/**
	 * If the original template's file name prefix is not the template's name, rename it. Also, rename the properties
	 * and overrides files if exist.
	 * 
	 * @param templatesFolder
	 *            the folder that contains the template's file.
	 * @param holder
	 *            holds the relevant template
	 * @throws IOException
	 *             If failed to rename.
	 */

	private void renameTemplateFilesIfNeeded(final File templatesFolder, final ComputeTemplateHolder holder)
			throws IOException {
		final String templateName = holder.getName();

		final String templateFileName = holder.getTemplateFileName();
		final File templateFile = new File(templatesFolder, templateFileName);
		final String propertiesFileName = holder.getPropertiesFileName();
		final String overridesFileName = holder.getOverridesFileName();

		try {
			String newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(templateFile, templateName,
					DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
			if (newName != null) {
				logger.log(Level.INFO, "[renameTemplateFileIfNeeded] - Renamed template file name from "
						+ templateFileName + " to " + newName + ".");
			}
			if (propertiesFileName != null) {
				final File propertiesFile = new File(templatesFolder, propertiesFileName);
				newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(propertiesFile, templateName,
						DSLUtils.TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX);
				if (newName != null) {
					logger.log(Level.INFO,
							"[renameTemplateFileIfNeeded] - Renamed template's properties file name from"
									+ " " + propertiesFileName + " to " + newName + ".");
				}
			}
			if (overridesFileName != null) {
				final File overridesFile = new File(templatesFolder, overridesFileName);
				newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(overridesFile, templateName,
						DSLUtils.TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX);
				if (newName != null) {
					logger.log(Level.INFO,
							"[renameTemplateFileIfNeeded] - Renamed template's overrides file name from "
									+ overridesFileName + " to " + newName + ".");
				}
			}
		} catch (final IOException e) {
			logger.log(Level.WARNING, "[renameTemplateFileIfNeeded] - Failed to rename template file name ["
					+ templateFile.getName() + "] to "
					+ templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX
					+ ". The file will be deleted. Error:" + e);
			// delete the groovy file to ensure the template file wont be copied.
			templateFile.delete();
			throw e;
		}
	}

	/**
	 * Gets the {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME} folder. Creates it if needed.
	 * 
	 * @return the folder.
	 */
	private File getTemplatesFolder() {
		final File templatesFolder = new File(cloudConfigurationDir,
				CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
		if (!cloudConfigurationDir.exists()) {
			templatesFolder.mkdir();
		}
		return templatesFolder;
	}

	private void validateAddTemplates(final AddTemplatesRequest request)
			throws RestErrorException {
		final AddTemplatesValidationContext validationContext = new AddTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setRequest(request);
		for (final AddTemplatesValidator validator : addTemplatesValidators) {
			validator.validate(validationContext);
		}
	}

	private void validateTemplateOperation(final String opName)
			throws RestErrorException {
		final TemplatesValidationContext validationContext = new TemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setOperationName(opName);
		for (final TemplatesValidator validator : templatesValidators) {
			validator.validate(validationContext);
		}
	}

	/**
	 * Removes a template from the cloud.
	 * 
	 * @param templateName
	 *            The name of the template to remove.
	 * @throws RestErrorException
	 *             If cloud is a local cloud or one of the REST instances failed to remove the template.
	 */
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "{templateName}", method = RequestMethod.DELETE)
	public void removeTemplate(@PathVariable final String templateName)
			throws RestErrorException {

		validateTemplateOperation("remove-template");

		logger.log(Level.INFO, "[removeTemplate] - removing template " + templateName);

		// check if the template is being used by at least one service, so it cannot be removed.
		final List<String> templateServices = getTemplateServices(templateName);
		if (!templateServices.isEmpty()) {
			logger.log(Level.WARNING, "[removeTemplate] - failed to remove template [" + templateName
					+ "]. The template is being used by " + templateServices.size() + " services: " + templateServices);
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(),
					templateName, templateServices);
		}
		// remove template from all REST instances
		final RemoveTemplatesResponse resposne = removeTemplateFromRestInstances(templateName);
		// check
		handleRemoveTemplateResponse(resposne, templateName);
		logger.log(Level.INFO, "[removeTemplate] - successfully removed template [" + templateName + "].");
	}

	private void handleRemoveTemplateResponse(final RemoveTemplatesResponse resposne, final String templateName)
			throws RestErrorException {
		final Map<String, String> failedToRemoveFromHosts = resposne.getFailedToRemoveFromHosts();
		final List<String> successfullyRemovedFromHosts = resposne.getSuccessfullyRemovedFromHosts();

		// check if some REST instances failed to remove the template
		if (!failedToRemoveFromHosts.isEmpty()) {
			String message = "[removeTemplate] - failed to remove template [" + templateName + "] from: "
					+ failedToRemoveFromHosts;
			if (!successfullyRemovedFromHosts.isEmpty()) {
				message += ". Succeeded to remove the template from: " + successfullyRemovedFromHosts;
			}
			logger.log(Level.WARNING, message);
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE.getName(),
					templateName, failedToRemoveFromHosts.toString());
		}
	}

	private List<String> getTemplateServices(final String templateName) {
		final List<String> services = new LinkedList<String>();
		final ProcessingUnits processingUnits = admin.getProcessingUnits();
		for (final ProcessingUnit processingUnit : processingUnits) {
			final Properties puProps = processingUnit.getBeanLevelProperties().getContextProperties();
			final String puTemplateName = puProps.getProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE);
			if (puTemplateName != null && puTemplateName.equals(templateName)) {
				services.add(processingUnit.getName());
			}
		}
		return services;
	}

	private RemoveTemplatesResponse removeTemplateFromRestInstances(final String templateName) {
		// get rest instances
		final ProcessingUnit processingUnit =
				admin.getProcessingUnits().waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
		final ProcessingUnitInstance[] instances = processingUnit.getInstances();
		// invoke remove-template command on each REST instance.
		logger.log(Level.INFO, "[removeTemplateFromRestInstances] - sending an http request to "
				+ instances.length + " REST instances. Template's name is " + templateName);
		final Map<String, String> failedToRemoveFromHosts = new HashMap<String, String>();
		final List<String> successfullyRemovedFromHosts = new LinkedList<String>();
		for (final ProcessingUnitInstance puInstance : instances) {
			String hostAddress = puInstance.getMachine().getHostAddress();
			final String port = Integer.toString(puInstance.getJeeDetails().getPort());
			try {
				RestClient restClient = createRestClient(hostAddress, port);
				restClient.removeTemplateInternal(templateName);
			} catch (final RestClientException e) {
				failedToRemoveFromHosts.put(hostAddress, e.getMessageFormattedText());
				logger.log(Level.WARNING, "[removeTemplateFromRestInstances] - remove template ["
					+ templateName + "] from instance [" + hostAddress + "] failed. Error: " 
					+ e.getMessageFormattedText(), e);
				continue;
			}
			successfullyRemovedFromHosts.add(hostAddress);
			logger.log(Level.INFO, "[removeTemplateFromRestInstances] - Successfully removed template ["
					+ templateName + "] from " + hostAddress);
		}
		final RemoveTemplatesResponse response = new RemoveTemplatesResponse();
		response.setFailedToRemoveFromHosts(failedToRemoveFromHosts);
		response.setSuccessfullyRemovedFromHosts(successfullyRemovedFromHosts);
		return response;
	}

	/**
	 * Internal method. Remove template file from the cloud configuration directory and from the cloud's templates map.
	 * This method supposed to be invoked from removeTemplate of a REST instance.
	 * 
	 * @param templateName
	 *            the name of the template to remove.
	 * @throws RestErrorException
	 *             If failed to remove the template.
	 */
	@InternalMethod
	// @PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "internal/{templateName}", method = RequestMethod.DELETE)
	public void
			removeTemplateInternal(@PathVariable final String templateName)
					throws RestErrorException {
		logger.log(Level.INFO, "[removeTemplateInternal] - removing template [" + templateName + "].");
		// check if the template is being used by at least one service, so it cannot be removed.
		final List<String> templateServices = getTemplateServices(templateName);
		if (!templateServices.isEmpty()) {
			logger.log(Level.WARNING, "[removeTemplateInternal] - failed to remove template [" + templateName
					+ "]. The template is being used by the following services: " + templateServices);
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(),
					templateName, templateServices);
		}
		// try to remove the template
		try {
			removeTemplateFromCloud(templateName);
		} catch (final RestErrorException e) {
			logger.log(Level.WARNING, "[removeTemplateInternal] - failed to remove template [" + templateName + "]."
					+ " Error: " + e.getMessage(), e);
			throw e;
		}
		logger.log(Level.INFO, "[removeTemplateInternal] - Successfully removed template [" + templateName + "].");
	}

	private void removeTemplateFromCloud(final String templateName)
			throws RestErrorException {

		logger.log(Level.INFO, "[removeTemplateFromCloud] - removing template [" + templateName + "] from cloud.");

		// delete template's file from the cloud configuration directory.
		deleteTemplateFile(templateName);

		// remove template from cloud
		final Map<String, ComputeTemplate> cloudTemplates = cloud.getCloudCompute().getTemplates();
		if (!cloudTemplates.containsKey(templateName)) {
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_NOT_EXIST.getName(), templateName);
		}
		cloudTemplates.remove(templateName);
	}

	/**
	 * Deletes the template's file. Deletes the templates folder if no other templates files exist in the folder.
	 * Deletes the {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME} folder if empty.
	 * 
	 * @param templateName
	 * @throws RestErrorException
	 */
	private void deleteTemplateFile(final String templateName) throws RestErrorException {
		final File templateFolder = getTemplateFolder(templateName);
		if (templateFolder == null) {
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templateName, "failed to get template's folder");
		}
		final File templateFile = getTemplateFile(templateName, templateFolder);
		if (templateFile == null) {
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templateName, "template file doesn't exist");
		}
		// delete the file from the directory.
		final String templatesPath = templateFile.getAbsolutePath();
		logger.log(Level.FINE, "[deleteTemplateFile] - removing template file " + templatesPath);

		boolean deleted = false;
		try {
			deleted = templateFile.delete();
		} catch (final SecurityException e) {
			logger.log(Level.WARNING, "[deleteTemplateFile] - Failed to deleted template file " + templatesPath
					+ ", Error: " + e.getMessage(), e);
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templatesPath, "Security exception: " + e.getMessage());
		}
		if (!deleted) {
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templatesPath, "template file was not deleted.");
		}
		logger.log(Level.FINE, "[deleteTemplateFile] - Successfully deleted template file [" + templatesPath + "].");
		// delete properties and overrides files if exist.
		ComputeTemplatesReader.removeTemplateFiles(templateFolder, templateName);
		deleteTemplateFolderIfNeeded(templateName, templateFolder);
	}

	private void deleteTemplateFolderIfNeeded(final String templateName, final File templateFolder) {
		logger.log(Level.FINE,
				"[deleteTemplateFile] - checking if the folder of template ["
						+ templateName + "] can be deleted [" + templateFolder + "].");
		final File[] templatesFiles =
				DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, templateFolder);
		if (templatesFiles == null || templatesFiles.length == 0) {
			// no other templates files in this folder
			try {
				logger.log(Level.FINE, "[deleteTemplateFile] - templates folder is empty, deleting it.");
				FileUtils.deleteDirectory(templateFolder);
			} catch (final IOException e) {
				logger.log(Level.WARNING, "[deleteTemplateFile] - Failed to delete templates folder"
						+ templateFolder, e);
			}
		} else {
			logger.log(Level.FINE, "[deleteTemplateFile] - templates folder is not empty.");
		}
	}

	private File getTemplateFolder(final String templateName) {
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
		final String absoluteUploadDir = computeTemplate.getAbsoluteUploadDir();
		final File parentFile = new File(absoluteUploadDir).getParentFile();
		if (parentFile == null) {
			logger.log(Level.WARNING, "Failed to get template's folder for template " + templateName
					+ ". The template's upload directory is " + absoluteUploadDir);
		}
		return parentFile;
	}

	/**
	 * Searches for a file with file name templateName-template.groovy in the given folder.
	 * 
	 * @param templateName
	 *            the name of the template (also the prefix of the wanted file).
	 * @return the found file or null.
	 */
	private File getTemplateFile(final String templateName, final File templateFolder) {
		final String templateFileName = templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX;
		logger.log(Level.FINE, "Searching for template file " + templateFileName + " in "
				+ templateFolder.getAbsolutePath());
		final File[] listFiles = templateFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return templateFileName.equals(name);
			}
		});
		final int length = listFiles.length;
		if (length == 0) {
			logger.log(Level.WARNING, "Didn't find template file with name " + templateName + " at "
					+ templateFolder.getAbsolutePath());
			return null;
		}
		if (length > 1) {
			logger.log(Level.WARNING, "Found " + length + " templates files with name " + templateName
					+ ": " + Arrays.toString(listFiles) + ". Returning the first one found.");
		}
		return listFiles[0];
	}
	
	
	/**
	 * Returns the name of the protocol used for communication with the rest server. If the security is secure (SSL)
	 * returns "https", otherwise returns "http".
	 *
	 * @param isSecureConnection
	 *            Indicates whether SSL is used or not.
	 * @return "https" if this is a secure connection, "http" otherwise.
	 */
	private static String getRestProtocol(final boolean isSecureConnection) {
		if (isSecureConnection) {
			return "https";
		}
		return "http";
	}

	private RestClient createRestClient(final String host, final String port) 
			throws RestClientException {
		final String protocol = getRestProtocol(permissionEvaluator != null);
		final String baseUrl = protocol + "://" + host + ":" + port;
		final String apiVersion = PlatformVersion.getVersion();
		try {
			return new RestClient(new URL(baseUrl), "", "", apiVersion);
		} catch (MalformedURLException e) {
			throw new RestClientException(CloudifyErrorMessages.FAILED_CREATE_REST_CLIENT.getName(), 
					"failed to create REST client", ExceptionUtils.getFullStackTrace(e));
		}
	}
}
