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
import java.net.MalformedURLException;
import java.net.URL;
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
import org.cloudifysource.domain.ComputeTemplateHolder;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.request.AddTemplatesInternalRequest;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplateResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesInternalResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesStatus;
import org.cloudifysource.dsl.rest.response.GetTemplateResponse;
import org.cloudifysource.dsl.rest.response.ListTemplatesResponse;
import org.cloudifysource.dsl.rest.response.RemoveTemplatesResponse;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.internal.RestClientInternal;
import org.cloudifysource.rest.repo.UploadRepo;
import org.cloudifysource.rest.util.RestUtils;
import org.cloudifysource.rest.validators.AddTemplatesValidationContext;
import org.cloudifysource.rest.validators.AddTemplatesValidator;
import org.cloudifysource.rest.validators.RemoveTemplateValidator;
import org.cloudifysource.rest.validators.RemoveTemplatesValidationContext;
import org.cloudifysource.rest.validators.TemplatesValidationContext;
import org.cloudifysource.rest.validators.TemplatesValidator;
import org.cloudifysource.restDoclet.annotations.InternalMethod;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.security.CustomPermissionEvaluator;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;
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
	private final RemoveTemplateValidator[] removeTemplatesValidators = new RemoveTemplateValidator[0];
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
		log(Level.INFO, "Initializing Templates controller.");
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
	 *             if failed to validate the addTemplates request.
	 * @throws IOException
	 *             If failed to unzip templates folder.
	 * @throws DSLException
	 *             If failed to read the templates from templates folder.
	 * @throws AddTemplatesException 
	 * 			   If failed to add templates (failure or partial failure).
	 */
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(method = RequestMethod.POST)
	public AddTemplatesResponse addTemplates(@RequestBody final AddTemplatesRequest request)
			throws RestErrorException, IOException, DSLException, AddTemplatesException {
		log(Level.INFO, "[addTemplates] - starting add templates.");

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
			final List<String> expectedTemplates = internalRequest.getExpectedTemplates();
			log(Level.INFO, "expecting to add " + expectedTemplates.size() + " templates: " + expectedTemplates);
			// add the templates to all REST instances
			final AddTemplatesResponse addTemplatesToRestInstances = 
					addTemplatesToRestInstances(internalRequest, templatesZippedFolder);
			handleAddTemplatesResponse(addTemplatesToRestInstances);
			return addTemplatesToRestInstances;
		} finally {
			if (templatesZippedFolder != null) {
				FileUtils.deleteQuietly(templatesZippedFolder);
			}
		}

	}

	private void handleAddTemplatesResponse(final AddTemplatesResponse addTemplatesResponse) 
			throws AddTemplatesException {
		final Map<String, AddTemplateResponse> templatesResponse = addTemplatesResponse.getTemplates();
		
		boolean atLeastOneFailed = false;
		boolean atLeastOneSucceeded = false;
		for (final AddTemplateResponse templateResponse : templatesResponse.values()) {
			final Map<String, String> failedToAddHosts = templateResponse.getFailedToAddHosts();
			if (failedToAddHosts != null && !failedToAddHosts.isEmpty()) {
				atLeastOneFailed = true;
				if (atLeastOneSucceeded) {
					break;
				}
			}
			final List<String> successfullyAddedHosts = templateResponse.getSuccessfullyAddedHosts();
			if (successfullyAddedHosts != null && !successfullyAddedHosts.isEmpty()) {
				atLeastOneSucceeded = true;
				if (atLeastOneFailed) {
					break;
				}
			}
		}
		/*
		 * partial failure or failure
		 */
		if (atLeastOneFailed) {
			if (atLeastOneSucceeded) {				
				// partial
				log(Level.WARNING,
						"[addTemplates] - Partial failure: " + templatesResponse);
				addTemplatesResponse.setStatus(AddTemplatesStatus.PARTIAL_FAILURE);
				throw new AddTemplatesException(addTemplatesResponse);
			}
			// failure
			log(Level.WARNING,
					"[addTemplates] - Failed to add all templates: " + templatesResponse);
			addTemplatesResponse.setStatus(AddTemplatesStatus.FAILURE);
			throw new AddTemplatesException(addTemplatesResponse);
		}
		addTemplatesResponse.setStatus(AddTemplatesStatus.SUCCESS);
		log(Level.INFO, "[addTemplatesToRestInstances] - successfully added all templates to all (" 
				+ addTemplatesResponse.getInstances().size() + ") REST instances.");

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
		log(Level.FINE, "listTemplates found " + templates.size() + " templates: " + templates);
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
			log(Level.WARNING, "[getTemplate] - template [" + templateName
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
		// cloud templates
		final File unzippedFolder = new ComputeTemplatesReader().unzipCloudTemplatesFolder(templatesZippedFolder);
		try {
			final List<ComputeTemplateHolder> cloudTemplatesHolders =
					new ComputeTemplatesReader().readCloudTemplatesFromDirectory(unzippedFolder);
			internalRequest.setCloudTemplates(cloudTemplatesHolders);
			// expected templates
			final List<String> expectedAddedTemplates = new LinkedList<String>();
			for (final ComputeTemplateHolder templateHolder : cloudTemplatesHolders) {
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
	 */
	private AddTemplatesResponse addTemplatesToRestInstances(final AddTemplatesInternalRequest request, 
			final File templatesZippedFolder) {

		final Map<String, AddTemplateResponse> templatesResponse = new HashMap<String, AddTemplateResponse>();
		
		// get the instances
		final ProcessingUnitInstance[] instances = admin.getProcessingUnits().
				waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS).getInstances();
		final List<String> instancesList = new ArrayList<String>(instances.length);
		// execute add-template on each rest instance
		log(Level.INFO, "[addTemplatesToRestInstances] - sending add-templates request to "
				+ instances.length + " instances.");
		for (final ProcessingUnitInstance puInstance : instances) {
			final String hostAddress = puInstance.getMachine().getHostAddress();
			instancesList.add(hostAddress);
			log(Level.INFO, "[addTemplatesToRestInstances] - sending request to " + hostAddress);
			/*
			 * add template to instance and get the response
			 */
			final AddTemplatesInternalResponse instanceResponse =
					executeAddTemplateOnInstance(
							hostAddress, Integer.toString(puInstance.getJeeDetails().getPort()), 
							request, templatesZippedFolder);
			final Map<String, String> failedToAddTempaltesToHost = instanceResponse.getFailedToAddTempaltesAndReasons();
			final List<String> addedTempaltes = instanceResponse.getAddedTempaltes();
			/*
			 * failed to add templates
			 */
			if (failedToAddTempaltesToHost != null) {
				for (final Entry<String, String> entry : failedToAddTempaltesToHost.entrySet()) {
					log(Level.WARNING, "[addTemplatesToRestInstances] - failed to add templates to host ["
							+ hostAddress + "]: " + failedToAddTempaltesToHost);
					// update template's entry in the final response
					// for each template - add the current host to the failure hosts map of the template.
					String templateName = entry.getKey();
					AddTemplateResponse addTemplateResponse = templatesResponse.get(templateName);
					// create new response if the template doesn't have one yet.
					if (addTemplateResponse == null) {
						addTemplateResponse = new AddTemplateResponse();
					}
					// get the failure map (hosts and reasons).
					Map<String, String> failedHostsReasons = addTemplateResponse.getFailedToAddHosts();
					if (failedHostsReasons == null) {
						failedHostsReasons = new HashMap<String, String>();
					}
					// add the failed host (and failure reason) to the failure map.
					failedHostsReasons.put(hostAddress, entry.getValue());
					// set the updated failure map at template's response.
					addTemplateResponse.setFailedToAddHosts(failedHostsReasons);
					// add the template and its response to the final templates response.
					templatesResponse.put(templateName, addTemplateResponse);
				}
			}
			/*
			 * successfully added templates
			 */
			if (addedTempaltes != null) {
				log(Level.INFO, "[addTemplatesToRestInstances] - successfully added templates to host ["
						+ hostAddress + "]: " + addedTempaltes);
				for (final String templateName : addedTempaltes) {
					AddTemplateResponse addTemplateResponse = templatesResponse.get(templateName);
					// create new response if the template doesn't have one yet.
					if (addTemplateResponse == null) {
						addTemplateResponse = new AddTemplateResponse();
					}
					// get the successfully hosts list.
					List<String> successfullyAddedHosts = addTemplateResponse.getSuccessfullyAddedHosts();
					if (successfullyAddedHosts == null) {
						successfullyAddedHosts = new LinkedList<String>();
					}
					// add the host to the successfully added hosts list.
					successfullyAddedHosts.add(hostAddress);
					// set the updated list at template's response.
					addTemplateResponse.setSuccessfullyAddedHosts(successfullyAddedHosts);
					// add the template and its response to the final templates response.
					templatesResponse.put(templateName, addTemplateResponse);
				}
			}
		}

		// create and return the response (the status of the response will be set later).
		final AddTemplatesResponse response = new AddTemplatesResponse();
		response.setInstances(instancesList);
		response.setTemplates(templatesResponse);
		return response;
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
			final AddTemplatesInternalRequest request,
			final File templatesZippedFolder) {
		AddTemplatesInternalResponse instanceResponse;
		String requestName = "create rest client";
		try {
			// invoke upload and add-templates commands on each REST instance.
			/*
			 * create rest client
			 */
			final RestClientInternal client = createRestClientInternal(host, port);
			requestName = "execute upload request";
			/*
			 * upload
			 */
			String uploadKey = client.uploadInternal(null, templatesZippedFolder).getUploadKey();
			log(Level.FINE, "[executeAddTemplateOnInstance] - Uploaded templates zipped folder [" 
					+ templatesZippedFolder + "] to host [" + host + "], upload key = " + uploadKey);
			request.setUploadKey(uploadKey);
			requestName = "execute add-templates-internal request";
			/*
			 * add templates
			 */
			instanceResponse = client.addTemplatesInternal(request);
		} catch (final RestClientException e) {
			// the request failed => all expected templates failed to be added
			// create a response that contains all expected templates in a failure map.
			log(Level.WARNING, "[executeAddTemplateOnInstance] - Failed to " + requestName + " to "
					+ host + ". Error message: " + e.getMessageFormattedText() + ", verbose: " + e.getVerbose());
			final Map<String, String> failedMap = new HashMap<String, String>();
			for (final String expectedTemplate : request.getExpectedTemplates()) {
				failedMap.put(expectedTemplate, "http request failed [" + e.getMessageFormattedText() + "]");
			}
			instanceResponse = new AddTemplatesInternalResponse();
			instanceResponse.setFailedToAddTempaltesAndReasons(failedMap);
			return instanceResponse;
		}
		final List<String> addedTempaltes = instanceResponse.getAddedTempaltes();
		log(Level.FINE, "[executeAddTemplateOnInstance] - added "
				+ addedTempaltes.size() + " templates: " + addedTempaltes);
		final Map<String, String> failedToAddTempaltesAndReasons =
				instanceResponse.getFailedToAddTempaltesAndReasons();
		final List<String> failedList = new ArrayList<String>(failedToAddTempaltesAndReasons.keySet());
		log(Level.FINE, "[executeAddTemplateOnInstance] - failed to add "
				+ failedList.size() + " templates: " + failedList);
		// addedTempaltes and failedList suppose to contain all templates from expectedTemplates.
		final List<?> union = ListUtils.union(addedTempaltes, failedList);
		final List<?> subtract = ListUtils.subtract(request.getExpectedTemplates(), union);
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
	 * @throws RestErrorException .
	 */
	@InternalMethod
	@RequestMapping(value = "internal", method = RequestMethod.POST)
	public AddTemplatesInternalResponse
			addTemplatesInternal(
					@RequestBody final AddTemplatesInternalRequest request)
					throws IOException, RestErrorException {

		final ComputeTemplatesReader reader = new ComputeTemplatesReader();
		String uploadKey = request.getUploadKey();
		final File templatesFolder = repo.get(uploadKey);
		if (templatesFolder == null) {
			throw new RestErrorException(CloudifyMessageKeys.WRONG_TEMPLATES_UPLOAD_KEY.getName(), uploadKey);
		}
		final File unzippedTemplatesFolder = reader.unzipCloudTemplatesFolder(templatesFolder);

		try {
			log(Level.INFO, "[addTemplatesInternal] - adding templates " + request.getExpectedTemplates());
			// add templates to the cloud and return the added templates.
			return addTemplatesToCloud(unzippedTemplatesFolder, request.getCloudTemplates());
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
		log(Level.FINE,
				"[addTemplatesToCloud] - Adding " + templatesHolders.size() + " templates to cloud.");
		// adds the templates to the cloud's templates list, deletes failed to added templates from the folder.
		final AddTemplatesInternalResponse addTemplatesToCloudListresponse =
				addTemplatesToCloudList(templatesFolder, templatesHolders);
		List<String> addedTemplates = addTemplatesToCloudListresponse.getAddedTempaltes();
		final Map<String, String> failedToAddTemplates =
				addTemplatesToCloudListresponse.getFailedToAddTempaltesAndReasons();
		// if no templates were added, throw an exception
		if (addedTemplates.isEmpty()) {
			log(Level.WARNING,
					"[addTemplatesToCloud] - Failed to add templates from " + templatesFolder.getAbsolutePath());
		} else {
			// at least one template was added, copy files from template folder to a new folder.
			log(Level.FINE,
					"[addTemplatesToCloud] - Coping templates files from " + templatesFolder.getAbsolutePath()
							+ " to a new folder under " + cloudConfigurationDir.getAbsolutePath());
			try {
				final File localTemplatesDir = copyTemplateFilesToCloudConfigDir(templatesFolder);
				log(Level.FINE, "[addTemplatesToCloud] - The templates files were copied to "
						+ localTemplatesDir.getAbsolutePath());
				updateCloudTemplatesUploadPath(addedTemplates, localTemplatesDir);
			} catch (final IOException e) {
				// failed to copy files - remove all added templates from cloud and them to the failed map.
				log(Level.WARNING,
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
			log(Level.WARNING, "[addTemplatesToCloud] - Failed to add the following templates: "
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
		log(Level.FINE,
				"[addTemplatesToCloudList] - adding " + cloudTemplates.size() + " templates to cloud's list.");
		for (final ComputeTemplateHolder holder : cloudTemplates) {
			final String templateName = holder.getName();
			final String originalTemplateFileName = holder.getTemplateFileName();
			// check if template already exist
			final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
			if (templates.containsKey(templateName)) {
				// template already exists
				log(Level.WARNING, "[addTemplatesToCloudList] - Template already exists: " + templateName);
				failedToAddTemplates.put(templateName, "template already exists");
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			
			// rename template file to <templateName>-template.groovy if needed
			// rename the properties and overrides files as well.
			try {
				renameTemplateFilesIfNeeded(templatesFolder, holder);
			} catch (final IOException e) {
				failedToAddTemplates.put(templateName, "failed to rename template's file. error: " + e.getMessage());
				// rename failed - delete the file so it wont be added to the additional templates folder.
				log(Level.WARNING, "[renameTemplateFileIfNeeded] - Failed to rename template's file."
						+ " The file [" + originalTemplateFileName + "] will be deleted.", e);
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
		final File templatesDirParent = restConfig.getAdditionalTempaltesFolder();
		// create new templates folder with a unique name.
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

		log(Level.FINE, "[renameTemplateFileIfNeeded] - Renaming template files [template name = " 
				+ templateName + "]");
		
		// rename groovy file if needed
		String newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(templateFile, templateName,
				DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
		if (newName != null) {
			log(Level.FINE, "[renameTemplateFileIfNeeded] - Renamed template file name from "
					+ templateFileName + " to " + newName + ".");
		}
		// rename properties file if needed
		if (propertiesFileName != null) {
			final File propertiesFile = new File(templatesFolder, propertiesFileName);
			newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(propertiesFile, templateName,
					DSLUtils.TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX);
			if (newName != null) {
				log(Level.FINE, "[renameTemplateFileIfNeeded] - Renamed template's properties file name from"
						+ " " + propertiesFileName + " to " + newName + ".");
			}
		}
		// rename overrides file if needed
		if (overridesFileName != null) {
			final File overridesFile = new File(templatesFolder, overridesFileName);
			newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(overridesFile, templateName,
					DSLUtils.TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX);
			if (newName != null) {
				log(Level.FINE, "[renameTemplateFileIfNeeded] - Renamed template's overrides file name from "
						+ overridesFileName + " to " + newName + ".");
			}
		}
		
	}

	private void validateRemoveTemplate(final String templateName) 
			throws RestErrorException {
		RemoveTemplatesValidationContext validationContext = new RemoveTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setTemplateName(templateName);
		validationContext.setCloudDeclaredTemplates(restConfig.getCloudDeclaredTemplates());
		validationContext.setAdmin(admin);
		for (final RemoveTemplateValidator validator : removeTemplatesValidators) {
			validator.validate(validationContext);
		}
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

		log(Level.INFO, "[removeTemplate] - starting remove template [" + templateName + "]");

		// validate
		validateRemoveTemplate(templateName);

		// remove template from all REST instances
		final RemoveTemplatesResponse resposne = removeTemplateFromRestInstances(templateName);
		handleRemoveTemplateResponse(resposne, templateName);
		log(Level.INFO, "[removeTemplate] - successfully removed template [" + templateName + "].");
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
			log(Level.WARNING, message);
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
		log(Level.INFO, "[removeTemplateFromRestInstances] - sending remove-template request to "
				+ instances.length + " REST instances.");
		final Map<String, String> failedToRemoveFromHosts = new HashMap<String, String>();
		final List<String> successfullyRemovedFromHosts = new LinkedList<String>();
		for (final ProcessingUnitInstance puInstance : instances) {
			final String hostAddress = puInstance.getMachine().getHostAddress();
			final String port = Integer.toString(puInstance.getJeeDetails().getPort());
			try {
				final RestClientInternal client = createRestClientInternal(hostAddress, port);
				log(Level.INFO, "sending request to " + hostAddress);
				client.removeTemplateInternal(templateName);
			} catch (final RestClientException e) {
				failedToRemoveFromHosts.put(hostAddress, e.getMessageFormattedText());
				log(Level.WARNING, "[removeTemplateFromRestInstances] - remove template ["
						+ templateName + "] from instance [" + hostAddress + "] failed. Error: "
						+ e.getMessageFormattedText(), e);
				continue;
			}
			successfullyRemovedFromHosts.add(hostAddress);
			log(Level.INFO, "[removeTemplateFromRestInstances] - Successfully removed template ["
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
	@RequestMapping(value = "internal/{templateName}", method = RequestMethod.DELETE)
	public void
			removeTemplateInternal(@PathVariable final String templateName)
					throws RestErrorException {
		log(Level.INFO, "[removeTemplateInternal] - removing template [" + templateName + "].");
		
		// validate
		validateRemoveTemplate(templateName);
		
		// try to remove the template
		try {
			removeTemplateFromCloud(templateName);
		} catch (final RestErrorException e) {
			log(Level.WARNING, "[removeTemplateInternal] - failed to remove template [" + templateName + "].", e);
			throw e;
		}
		log(Level.INFO, "[removeTemplateInternal] - Successfully removed template [" + templateName + "].");
	}

	private void removeTemplateFromCloud(final String templateName)
			throws RestErrorException {
		log(Level.FINE, "[removeTemplateFromCloud] - removing template [" + templateName + "] from cloud.");
		// delete template's file from the cloud configuration directory.
		try {
			deleteTemplateFile(templateName);
		} catch (final RestErrorException e) {
			log(Level.WARNING, "[removeTemplateFromCloud] - failed to remove template's files: "
					+ e.getLocalizedMessage() + ". The template will not be removed from the cloud's tempaltes list.");
			throw e;
		}
		// remove template from cloud's list
		removeTemplateFromCloudList(templateName);
	}

	private void removeTemplateFromCloudList(final String templateName)
			throws RestErrorException {
		log(Level.FINE, "[removeTemplateFromCloudList] - removing template [" + templateName + "] from cloud's list.");
		cloud.getCloudCompute().getTemplates().remove(templateName);
		log(Level.FINE, "[removeTemplateFromCloudList] - template [" + templateName
				+ "] was removed from cloud's list.");
	}

	private void validateTemplateExist(final String templateName) 
			throws RestErrorException {
		final Map<String, ComputeTemplate> cloudTemplates = cloud.getCloudCompute().getTemplates();
		if (!cloudTemplates.containsKey(templateName)) {
			log(Level.WARNING,
					"[validateTemplateExist] - tempalte [" + templateName + "] doesn't exist in cloud's list.");
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_NOT_EXIST.getName(), templateName);
		}
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
		log(Level.FINE, "[deleteTemplateFile] - removing template file " + templatesPath);

		boolean deleted = false;
		try {
			deleted = templateFile.delete();
		} catch (final SecurityException e) {
			log(Level.WARNING, "[deleteTemplateFile] - Failed to deleted template file " + templatesPath
					+ ", Error: " + e.getMessage(), e);
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templatesPath, "Security exception: " + e.getMessage());
		}
		if (!deleted) {
			throw new RestErrorException(CloudifyErrorMessages.FAILED_REMOVE_TEMPLATE_FILE.getName(),
					templatesPath, "template file was not deleted.");
		}
		log(Level.FINE, "[deleteTemplateFile] - Successfully deleted template file [" + templatesPath + "].");
		// delete properties and overrides files if exist.
		ComputeTemplatesReader.removeTemplateFiles(templateFolder, templateName);
		deleteTemplateFolderIfNeeded(templateName, templateFolder);
	}

	private void deleteTemplateFolderIfNeeded(final String templateName, final File templateFolder) {
		log(Level.FINE,
				"[deleteTemplateFile] - checking if the folder of template ["
						+ templateName + "] can be deleted [" + templateFolder + "].");
		final File[] templatesFiles =
				DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, templateFolder);
		if (templatesFiles == null || templatesFiles.length == 0) {
			// no other templates files in this folder
			try {
				log(Level.FINE, "[deleteTemplateFile] - templates folder is empty, deleting it.");
				FileUtils.deleteDirectory(templateFolder);
			} catch (final IOException e) {
				log(Level.WARNING, "[deleteTemplateFile] - Failed to delete templates folder"
						+ templateFolder, e);
			}
		} else {
			log(Level.FINE, "[deleteTemplateFile] - templates folder is not empty.");
		}
	}

	private File getTemplateFolder(final String templateName) {
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(templateName);
		final String absoluteUploadDir = computeTemplate.getAbsoluteUploadDir();
		log(Level.FINE, "[getTemplateFolder] - template's [" + templateName
				+ "] upload directory: " + absoluteUploadDir);
		final File parentFile = new File(absoluteUploadDir).getParentFile();
		if (parentFile == null) {
			log(Level.WARNING, "[getTemplateFolder] - Failed to get template's folder for template " + templateName
					+ ". The template's upload directory is " + absoluteUploadDir);
		}
		log(Level.FINE, "[getTemplateFolder] - Found the folder for template [" 
				+ templateName + "] - " + parentFile.getAbsolutePath());
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
		log(Level.FINE, "[getTemplateFile] - Searching for template file " + templateFileName + " in "
				+ templateFolder.getAbsolutePath());
		final File[] listFiles = templateFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return templateFileName.equals(name);
			}
		});
		final int length = listFiles.length;
		if (length == 0) {
			log(Level.WARNING, "Didn't find template file with name " + templateName + " at "
					+ templateFolder.getAbsolutePath());
			return null;
		}
		if (length > 1) {
			log(Level.WARNING, "Found " + length + " templates files with name " + templateName
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

	private RestClientInternal createRestClientInternal(final String host, final String port)
			throws RestClientException {
		final String protocol = getRestProtocol(permissionEvaluator != null);
		final String baseUrl = protocol + "://" + IPUtils.getSafeIpAddress(host) + ":" + port;
		final String apiVersion = PlatformVersion.getVersion();
		try {
			return new RestClientInternal(new URL(baseUrl), "", "", apiVersion);
		} catch (final MalformedURLException e) {
			throw MessagesUtils.createRestClientException(
					ExceptionUtils.getFullStackTrace(e),
					CloudifyErrorMessages.FAILED_CREATE_REST_CLIENT.getName(), 
					ExceptionUtils.getFullStackTrace(e));
		}
	}

	private void log(final Level level, final String content) {
		if (logger.isLoggable(level)) {
			logger.log(level, content);
		}
	}

	private void log(final Level level, final String content, final Throwable thrown) {
		if (logger.isLoggable(level)) {
			logger.log(level, content, thrown);
		}
	}

}
