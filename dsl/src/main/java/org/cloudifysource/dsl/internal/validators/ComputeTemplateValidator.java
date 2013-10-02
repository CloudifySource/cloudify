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
package org.cloudifysource.dsl.internal.validators;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class ComputeTemplateValidator implements DSLValidator {

	private ComputeTemplate entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (ComputeTemplate) dslEntity;
		
	}
	
	/**
	 * 
	 * @param context - validation context.
	 * @throws DSLValidationException - in case validation failed.
	 */
	@DSLValidation
	public void validateDefaultValues(final DSLValidationContext context)
			throws DSLValidationException {
		if (this.entity.getRemoteDirectory() == null || this.entity.getRemoteDirectory().trim().isEmpty()) {
			throw new DSLValidationException("Remote directory for template is missing");
		}

		if (StringUtils.isBlank(this.entity.getLocalDirectory())) {
			throw new DSLValidationException("Local directory for template is missing");
		}

		if ("ENTER_KEY_FILE_NAME".equals(this.entity.getKeyFile())) {
			throw new DSLValidationException(
					"Key file name field still has default configuration value of ENTER_KEY_FILE_NAME");
		}

	}

	@DSLValidation
	void validateRelativeUploadDir(final DSLValidationContext context)
			throws DSLValidationException {
		final File uploadDir = findUploadDir(context);

		// check key file!
		if (StringUtils.isNotBlank(this.entity.getKeyFile())) {
			final File keyFile = new File(uploadDir, this.entity.getKeyFile());
			if (!keyFile.exists() || !keyFile.isFile()) {
				throw new DSLValidationException("The specified key file was not found: " + keyFile);
			}

		}

		// this.localDirectory = uploadDir.getAbsolutePath();
		// logger.info("SETTING LOCAL DIRECTORY TO ABSOLUTE PATH: " +
		// this.localDirectory);

	}
	
	/************
	 * This is a unique situation: we need two pieces of information - the absolute location of the local directory, and
	 * the relative location of the local directory. So this validation fills in this field - note that the absolute
	 * field does not have a setter - groovy files can't directly set this value.
	 *
	 * @param context
	 *            .
	 * @throws DSLValidationException .
	 */
	@DSLValidation
	void validateAbsoluteUploadDir(final DSLValidationContext context)
			throws DSLValidationException {
		if (entity.getAbsoluteUploadDir() != null) {
			throw new DSLValidationException("absolute upload directory may not be set by external code");
		}
//		logger.fine("SETTING ABSOLUTE LOCAL UPLOAD DIRECTORY TO ABSOLUTE PATH: " + this.entity.getAbsoluteUploadDir());
		this.entity.setAbsoluteUploadDir(findUploadDir(context).getAbsolutePath()); 

	}
	

	private File findUploadDir(final DSLValidationContext context)
			throws DSLValidationException {
		final File relativeUploadDir = new File(this.entity.getLocalDirectory());
		if (relativeUploadDir.isAbsolute()) {
			throw new DSLValidationException(
					"Upload directory of a cloud template must be a relative path, "
							+ "relative to the cloud configuration directory");
		}

		File dslDir = null;
		if (context.getFilePath() == null) {
			throw new IllegalStateException("The DSL File location is not set! Cannot validate!");
		} else {
			final File dslFile = new File(context.getFilePath());
			dslDir = dslFile.getParentFile();
		}

		final File uploadDir = new File(dslDir, entity.getLocalDirectory());
		if (!uploadDir.exists()) {
			throw new DSLValidationException(
					"Could not find upload directory at: " + uploadDir);
		}

		if (!uploadDir.isDirectory()) {
			throw new DSLValidationException(
					"Upload directory, set to: " + uploadDir + " is not a directory");
		}
		return uploadDir;
	}
	

	@DSLValidation
	public void validateOpenFilesLimitOnProvilegedMode(final DSLValidationContext context)
			throws DSLValidationException {
		if(this.entity.getOpenFilesLimit() == null) {
			return;
		}
		
		if(this.entity.isPrivileged()) {
			return;
		}
		
		

		
		throw new DSLValidationException("Setting an open files limit requires that the template run in privileged mode");
		
	}

}
