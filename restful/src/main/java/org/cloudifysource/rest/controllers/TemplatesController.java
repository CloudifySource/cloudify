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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.ComputeTemplateHolder;
import org.cloudifysource.dsl.internal.ComputeTemplatesReader;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.rest.request.AddTemplatesInternalRequest;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesPartialFailureResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesToPUResponse;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.controllers.helpers.ControllerHelper;
import org.cloudifysource.rest.repo.UploadRepo;
import org.cloudifysource.rest.util.RestUtils;
import org.cloudifysource.rest.validators.AddTemplatesValidationContext;
import org.cloudifysource.rest.validators.AddTemplatesValidator;
import org.cloudifysource.restDoclet.annotations.InternalMethod;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.security.CustomPermissionEvaluator;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
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
	private static final Logger logger = Logger.getLogger(DeploymentsController.class.getName());
	@Autowired
	private RestConfiguration restConfig;
	@Autowired
	private UploadRepo repo;
	@Autowired
	private final AddTemplatesValidator[] addTemplatesValidators = new AddTemplatesValidator[0];
	private Cloud cloud;
	private ControllerHelper controllerHelper;
	private CustomPermissionEvaluator permissionEvaluator;
	private File cloudConfigurationDir;

	/**
	 * Initialization.
	 */
	@PostConstruct
	public void init() {
		cloud = restConfig.getCloud();
		controllerHelper = new ControllerHelper(restConfig.getGigaSpace(), restConfig.getAdmin());
		permissionEvaluator = restConfig.getPermissionEvaluator();
		cloudConfigurationDir = restConfig.getCloudConfigurationDir();
	}

	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(method = RequestMethod.POST)
	public AddTemplatesResponse addTemplates(@RequestBody final AddTemplatesRequest request)
			throws RestErrorException, IOException, DSLException {
		logger.log(Level.INFO, "[addTemplates] - starting add templates.");
		// validate
		validateAddTemplates(request);

		// create an internal request to avoid 
		AddTemplatesInternalRequest internalRequest = createInternalRequest(request);
		// add the templates to the remote PUs
		return addTemplatesToRestInstances(internalRequest);
		
	}

	private AddTemplatesInternalRequest createInternalRequest(final AddTemplatesRequest request) 
					throws IOException, RestErrorException, DSLException {
		// get templates folder
		final String uploadKey = request.getUploadKey();
		final File templatesZippedFolder = repo.get(uploadKey);
		if (templatesZippedFolder == null) {
			throw new RestErrorException(CloudifyMessageKeys.WRONG_TEMPLATES_UPLOAD_KEY.getName(), uploadKey);
		}
		// unzip and add the templates from the folder.
		File templatesUnzippedFolder = null;
		try {
			templatesUnzippedFolder = new ComputeTemplatesReader().unzipCloudTemplatesFolder(templatesZippedFolder);
			final List<ComputeTemplateHolder> cloudTemplatesHolders = readCloudTemplates(templatesUnzippedFolder);
			final List<String> expectedAddedTemplates = 
					readCloudTemplatesNames(templatesUnzippedFolder, cloudTemplatesHolders);
			AddTemplatesInternalRequest internalRequest = new AddTemplatesInternalRequest();
			internalRequest.setUploadKey(uploadKey);
			internalRequest.setCloudTemplates(cloudTemplatesHolders);
			internalRequest.setExpectedTemplates(expectedAddedTemplates);
			return internalRequest;
		} finally {
			FileUtils.deleteQuietly(templatesUnzippedFolder);
			FileUtils.deleteQuietly(templatesZippedFolder);
		}
	}

	private void checkForFailure(final AddTemplatesPartialFailureResponse response)
			throws RestErrorException {
		
		Map<String, Map<String, String>> failedToAddAllTempaltesHosts = response.getFailedToAddAllTempaltesHosts();
		Map<String, AddTemplatesToPUResponse> partialFailedToAddTempaltesHosts = 
				response.getPartialFailedToAddTempaltesHosts();
		List<String> successfullyAddedAllTempaltesHosts = response.getSuccessfullyAddedAllTempaltesHosts();
		
		// If add templates failed to be added, throw an exception
		if (partialFailedToAddTempaltesHosts.isEmpty() && successfullyAddedAllTempaltesHosts.isEmpty()) {
			logger.log(Level.WARNING, 
					"[addTemplates] - Failed to add templates: " + failedToAddAllTempaltesHosts);
			throw new RestErrorException(CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName(),
					failedToAddAllTempaltesHosts); 
		}
	}

	/**
	 * Reads the templates from templatesFolder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @return the list of the read cloud templates.
	 * @throws RestErrorException
	 *             If no templates files were found.
	 * @throws DSLException
	 *             If failed to read templates.
	 */
	private List<String> readCloudTemplatesNames(final File templatesFolder, 
			final List<ComputeTemplateHolder> cloudTemplatesHolders) {
		final List<String> cloudTemplateNames = new LinkedList<String>();
		for (final ComputeTemplateHolder cloudTemplateHolder : cloudTemplatesHolders) {
			cloudTemplateNames.add(cloudTemplateHolder.getName());
		}
		return cloudTemplateNames;
	}

	/**
	 * Reads the templates from templatesFolder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @return the list of the read cloud templates.
	 * @throws RestErrorException
	 *             If no templates files were found.
	 * @throws DSLException
	 *             If failed to read templates.
	 */
	private List<ComputeTemplateHolder> readCloudTemplates(final File templatesFolder)
			throws RestErrorException, DSLException {
		List<ComputeTemplateHolder> cloudTemplatesHolders;
		final ComputeTemplatesReader reader = new ComputeTemplatesReader();
		cloudTemplatesHolders = reader.readCloudTemplatesFromDirectory(templatesFolder);
		if (cloudTemplatesHolders.isEmpty()) {
			throw new RestErrorException("no_template_files", "templates folder missing templates files.",
					templatesFolder.getAbsolutePath());
		}
		return cloudTemplatesHolders;
	}

	/**
	 * For each puInstance - send the add templates request.
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
	 * 			  If failed to add all templates (no template was added).
	 */
	private AddTemplatesResponse addTemplatesToRestInstances(final AddTemplatesInternalRequest request) 
			throws RestErrorException {
		
		Map<String, Map<String, String>> failedToAddAllTempaltesHosts = new HashMap<String, Map<String, String>>();
		Map<String, AddTemplatesToPUResponse> partialFailedToAddTempaltesHosts = 
				new HashMap<String, AddTemplatesToPUResponse>();
		List<String> successfullyAddedAllTempaltesHosts = new LinkedList<String>();
		List<String> expectedAddedTemplates = request.getExpectedTemplates();

		// get the instances
		final ProcessingUnitInstance[] instances = restConfig.getAdmin().getProcessingUnits().
				waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS).getInstances();
		logger.log(Level.INFO, "[sendAddTemplatesToRestInstances] - sending templates folder to "
				+ instances.length + " instances.");

		// send the templates folder to each rest instance
		for (final ProcessingUnitInstance puInstance : instances) {
			final String hostAddress = puInstance.getMachine().getHostAddress();
			final String host = puInstance.getMachine().getHostName() + "/" + hostAddress;
			final String port = Integer.toString(puInstance.getJeeDetails().getPort());
			
			AddTemplatesToPUResponse response;
			try {
				// send the post request
				final RestClient restClient =
						createRestClient(hostAddress, port, ""/* username */, ""/* password */);
				response = restClient.addTemplatesInternal(request);
			} catch (final Exception e) {
				logger.log(Level.WARNING, "[sendAddTemplatesToRestInstances] - failed to execute http request to "
						+ host + ". Error: " + e, e);
				final Map<String, String> failedMap = new HashMap<String, String>();
				// the request failed to be sent => all expected templates failed to be added
				for (final String expectedTemplate : request.getExpectedTemplates()) {
					failedMap.put(expectedTemplate, e.getMessage());
				}
				failedToAddAllTempaltesHosts.put(host, failedMap);
				continue;
			}
			final Map<String, String> failedMap = response.getFailedToAddTempaltesAndReasons();
			List<String> addedTempaltes = response.getAddedTempaltes();
			if (!failedMap.isEmpty() && !addedTempaltes.isEmpty()) {
				partialFailedToAddTempaltesHosts.put(host, response);
			} else if (!failedMap.isEmpty() && addedTempaltes.isEmpty()) {
				failedToAddAllTempaltesHosts.put(host, failedMap);
			} else {
				successfullyAddedAllTempaltesHosts.add(host);
			}
		}
		// create and return the response.
		final AddTemplatesPartialFailureResponse partialResponse = new AddTemplatesPartialFailureResponse();
		partialResponse.setFailedToAddAllTempaltesHosts(failedToAddAllTempaltesHosts);
		partialResponse.setPartialFailedToAddTempaltesHosts(partialFailedToAddTempaltesHosts);
		partialResponse.setSuccessfullyAddedAllTempaltesHosts(successfullyAddedAllTempaltesHosts);
	
		AddTemplatesResponse addTemplatesResponse = new AddTemplatesResponse();
		
		checkForFailure(partialResponse);
		addTemplatesResponse.setAddedTemplates(expectedAddedTemplates);
		if (!partialFailedToAddTempaltesHosts.isEmpty() 
				|| !failedToAddAllTempaltesHosts.isEmpty()) {	
			logger.log(Level.INFO, "[addTemplatesToRestInstances] - Partial success: " 
					+ printPartialSuccess(
							successfullyAddedAllTempaltesHosts, 
							failedToAddAllTempaltesHosts, 
							partialFailedToAddTempaltesHosts));
			addTemplatesResponse.setPartialFailureResponse(partialResponse);
		} else {
			// no failures - all expected templates was added to all hosts.
			logger.log(Level.INFO, "[addTemplatesToRestInstances] - Successfully added templates: " 
			+ expectedAddedTemplates);
			addTemplatesResponse.setAddedTemplates(expectedAddedTemplates);
		}
		return addTemplatesResponse;		
	}

	private String printPartialSuccess(
			final List<String> successfullyAddedAllTempaltesHosts,
			final Map<String, Map<String, String>> failedToAddAllTempaltesHosts,
			final Map<String, AddTemplatesToPUResponse> partialFailedToAddTempaltesHosts) {
		String toString = "";
		if (!successfullyAddedAllTempaltesHosts.isEmpty()) {
			toString += "The following hosts successfully added all expected templates:"
					+ successfullyAddedAllTempaltesHosts;
		}
		if (!failedToAddAllTempaltesHosts.isEmpty()) {
			if (!toString.isEmpty()) {
				toString += CloudifyConstants.NEW_LINE;
			}
			toString += "The following hosts failed to add templates (no template was added):" 
					+ CloudifyConstants.NEW_LINE 
					+ failedToAddAllTempaltesHosts;
		}
		if (!partialFailedToAddTempaltesHosts.isEmpty()) {
			if (!toString.isEmpty()) {
				toString += CloudifyConstants.NEW_LINE;
			}
			toString += "The following hosts failed to add all expected templates (some templates were added):" 
					+ CloudifyConstants.NEW_LINE 
					+ partialFailedToAddTempaltesHosts;
		}
		return toString;
	}

	private RestClient createRestClient(final String host, final String port, final String username,
			final String password) throws MalformedURLException, RestClientException {
		final String protocol = getRestProtocol(permissionEvaluator != null);
		final String baseUrl = protocol + "://" + host + ":" + port;
		final String versionName = PlatformVersion.getVersion() + "-Cloudify-" + PlatformVersion.getMilestone();
		return new RestClient(new URL(baseUrl), username, password, versionName);
	}

	/**
	 * Internal method. Add template files to the cloud configuration directory and to the cloud object. This method
	 * supposed to be invoked from addTemplates of a REST instance.
	 * 
	 * @param request
	 *            The request.
	 * @return a map containing the added templates and a success status if succeeded, else returns an error status.
	 * @throws RestErrorException
	 *             in case of failing to add the template to the space.
	 * @throws IOException
	 *             in case of reading error.
	 * @throws DSLException
	 *             in case of failing to read a DSL object.
	 */
	@InternalMethod
	// @PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "internal", method = RequestMethod.POST)
	public AddTemplatesToPUResponse
			addTemplatesInternal(
					@RequestBody
					final AddTemplatesInternalRequest request)
					throws IOException {
		final ComputeTemplatesReader reader = new ComputeTemplatesReader();
		final File templatesFolder = repo.get(request.getUploadKey());
		File unzippedTemplatesFolder = reader.unzipCloudTemplatesFolder(templatesFolder);

		final List<String> expectedAddedTemplates = 
				readCloudTemplatesNames(unzippedTemplatesFolder, request.getCloudTemplates());

		try {
			logger.log(Level.INFO, "[addTemplatesInternal] - adding templates from templates folder: "
					+ unzippedTemplatesFolder.getAbsolutePath());
			// add templates to the cloud and return the added templates.
			AddTemplatesToPUResponse addTemplatesToPUResponse = 
					addTemplatesToCloud(unzippedTemplatesFolder, request.getCloudTemplates());
			checkAllTempaltesAdded(addTemplatesToPUResponse, expectedAddedTemplates);
			return addTemplatesToPUResponse;
		} finally {
			FileUtils.deleteQuietly(unzippedTemplatesFolder);
		}
	}
	
	private void checkAllTempaltesAdded(final AddTemplatesToPUResponse addTemplatesToPUResponse, 
			final List<String> expectedAddedTemplates) {
		List<String> addedTempaltes = addTemplatesToPUResponse.getAddedTempaltes();
		List intersection = ListUtils.intersection(expectedAddedTemplates, addedTempaltes);
		if (intersection.size() != expectedAddedTemplates.size()) {
			
		}
		
	}

	/**
	 * Adds templates to cloud's templates. Adds templates' files to cloud configuration directory.
	 *
	 * @param templatesFolder
	 *            .
	 * @return a map contains the added templates list and the failed to add templates list.
	 */
	private AddTemplatesToPUResponse addTemplatesToCloud(final File templatesFolder, 
			final List<ComputeTemplateHolder> cloudTemplatesHolders) {

		logger.log(Level.FINE, "[addTemplatesToCloud] - Adding templates to cloud.");

		// adds the templates to the cloud's templates list, deletes the failed to added templates from the folder.
		AddTemplatesToPUResponse addTemplatesToPUResponse = 
				addTemplatesToCloudList(templatesFolder, cloudTemplatesHolders);
		final List<String> addedTemplates = addTemplatesToPUResponse.getAddedTempaltes();
		final Map<String, String> failedToAddTemplates = addTemplatesToPUResponse.getFailedToAddTempaltesAndReasons();
		// if no templates were added, throw an exception
		if (addedTemplates.isEmpty()) {
			logger.log(Level.WARNING, "[addTemplatesToCloud] - Failed to add templates files from "
					+ templatesFolder.getAbsolutePath());
		} else {
			// at least one template was added, copy files from template folder to cloudTemplateFolder
			logger.log(Level.INFO, "[addTemplatesToCloud] - Coping templates files from "
					+ templatesFolder.getAbsolutePath() + " to " 
					+ cloudConfigurationDir.getAbsolutePath());
			try {
				final File localTemplatesDir = copyTemplateFilesToCloudConfigDir(templatesFolder);
				updateCloudTemplatesUploadPath(addedTemplates, localTemplatesDir);
			} catch (final IOException e) {
				// failed to copy files - remove all added templates from cloud and them to the failed map.
				logger.log(Level.WARNING, "[addTemplatesToCloud] - Failed to copy templates files, error: "
						+ e.getMessage(), e);
				for (final String templateName : addedTemplates) {
					cloud.getCloudCompute().getTemplates().remove(templateName);
					failedToAddTemplates.put(templateName, "failed to copy templates files");
				}

			}
		}
		if (!failedToAddTemplates.isEmpty()) {
			logger.log(Level.INFO, "[addTemplatesToCloud] - Failed to add the following templates: "
					+ failedToAddTemplates.toString());
		}

		// create and return the result.
		AddTemplatesToPUResponse finalResponse = new AddTemplatesToPUResponse();
		finalResponse.setAddedTempaltes(addedTemplates);
		finalResponse.setFailedToAddTempaltesAndReasons(failedToAddTemplates);
		return finalResponse;
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
		for (String templateName : addedTemplates) {
			ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
			String localUploadPath = new File(localTemplatesDir, cloudTemplate.getLocalDirectory()).getAbsolutePath();
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
	private AddTemplatesToPUResponse addTemplatesToCloudList(
			final File templatesFolder, final List<ComputeTemplateHolder> cloudTemplates) {
		final List<String> addedTemplates = new LinkedList<String>();
		final Map<String, String> failedToAddTemplates = new HashMap<String, String>();
		for (ComputeTemplateHolder holder : cloudTemplates) {
			String templateName = holder.getName();
			String originalTemplateFileName = holder.getTemplateFileName();
			// check if template already exist
			if (cloud.getCloudCompute().getTemplates().containsKey(templateName)) {
				logger.log(Level.WARNING, "[addTemplatesToCloudList] - Template already exists: " + templateName);
				failedToAddTemplates.put(templateName, "template already exists");
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			// rename template file to <templateName>-template.groovy if needed
			// rename the proeprties and overrides files as well.
			try {
				renameTemplateFileIfNeeded(templatesFolder, holder);
			} catch (final IOException e) {
				logger.log(Level.WARNING, "[addTemplatesToCloudList] - Failed to rename template's file, template: "
						+ templateName + ", error: " + e.getMessage(), e);
				failedToAddTemplates.put(templateName, "failed to rename template's file. error: " + e.getMessage());
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			// add template to cloud templates list
			ComputeTemplate cloudTemplate = holder.getCloudTemplate();
			cloud.getCloudCompute().getTemplates().put(templateName, cloudTemplate);
			addedTemplates.add(templateName);
		}
		AddTemplatesToPUResponse addTemplatesToPUResponse = new AddTemplatesToPUResponse();
		addTemplatesToPUResponse.setAddedTempaltes(addedTemplates);
		addTemplatesToPUResponse.setFailedToAddTempaltesAndReasons(failedToAddTemplates);
		return addTemplatesToPUResponse;
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
		// create new templates folder - increment folder number until no folder
		// with that name exist.
		String folderName = "templates_" + restConfig.getLastTemplateFileNum().incrementAndGet();
		File copiedtemplatesFolder = new File(templatesDirParent, folderName);
		while (copiedtemplatesFolder.exists()) {
			folderName = "templates_" + restConfig.getLastTemplateFileNum().incrementAndGet();
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

	private void renameTemplateFileIfNeeded(final File templatesFolder, final ComputeTemplateHolder holder)
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
			// delete the groovy file to ensure the template file wont be
			// copied.
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

	private void validateAddTemplates(final AddTemplatesRequest request)
			throws RestErrorException {
		final AddTemplatesValidationContext validationContext = new AddTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setRequest(request);
		for (final AddTemplatesValidator validator : addTemplatesValidators) {
			validator.validate(validationContext);
		}
	}
}
