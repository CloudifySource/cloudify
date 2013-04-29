/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.rest.repo.UploadRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


/**
 * A controller for uploading files for future deployments.
 * Each uploaded file will be available for {@link CloudifyConstants#DEFAULT_UPLOAD_TIMEOUT_SECOND} seconds.
 * The timeout can be edited via {@link UploadRepo#setCleanupTimeoutSeconds(int)}. 
 * @author yael
 * @since 2.6.0
 *
 */
@Controller
@RequestMapping(value = "/upload")
public class UploadController {

	//@Value(value = "${upload.uploadSizeLimitBytes}")
	private static final int UPLOAD_SIZE_LIMIT_BYTE = 100000000;
	private int uploadSizeLimitBytes = UPLOAD_SIZE_LIMIT_BYTE;
	private static final Logger logger = Logger
			.getLogger(UploadController.class.getName());
	
	@Autowired
	private UploadRepo uploadRepo;
	
	/**
	 * Initializing the uploadRepo which responsible for uploading and retrieving the files.
	 * @throws IOException .
	 */
	@PostConstruct
	public void init() throws IOException {
		uploadRepo.init();
	}
	
	/**
	 * terminating the uploadRepo.
	 * @throws IOException .
	 */
	@PreDestroy
	public void destroy() throws IOException {
		uploadRepo.destroy();
	}
	
	/**
	 * Uploading a file to be used in future deployments.
	 * The file will be kept at least {@link UploadRepo#TIMEOUT_SECOND} seconds.
	 * @param fileName - the name of the file to upload.
	 * @param file - the file to upload.
	 * @return {@link UploadResponse} - contains the uploaded file's name.
	 * @throws RestErrorException 
	 */
	@PreAuthorize("isFullyAuthenticated() and hasPermission(#authGroups, 'deploy')")
	@RequestMapping(value = "/{fileName:.+}", method = RequestMethod.POST)
	@ResponseBody
	public UploadResponse upload(
			@PathVariable() final String fileName,
			@RequestParam(value = CloudifyConstants.UPLOAD_FILE_PARAM_NAME, required = true) final MultipartFile file) 
			throws RestErrorException {
		// determine file's name
		String name = fileName;
		if (fileName.isEmpty()) {
			name = file.getOriginalFilename();
		}
		
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to upload file " + name);
		}
		// enforce size limit
		long size = file.getSize();
		if (size > uploadSizeLimitBytes) {
			throw new RestErrorException(
					CloudifyMessageKeys.FILE_SIZE_LIMIT_EXCEEDED.getName(), name, uploadSizeLimitBytes, size);
		}
		// upload file using uploadRepo
		String uploadedFileDirName = null;
		try {
			uploadedFileDirName = uploadRepo.put(name, file);
		} catch (IOException e) {
			logger.warning("could not upload file " + name + " error was - " + e.getMessage());
			throw new RestErrorException(
					CloudifyMessageKeys.UPLOAD_FAILED.getName(), name, e.getMessage());
		} 
		// create and return UploadResponse
		UploadResponse response = new UploadResponse();
		response.setUploadKey(uploadedFileDirName);
		return response;
	}

	public int getUploadSizeLimitBytes() {
		return uploadSizeLimitBytes;
	}

	public void setUploadSizeLimitBytes(final int uploadSizeLimitBytes) {
		this.uploadSizeLimitBytes = uploadSizeLimitBytes;
	}
}
