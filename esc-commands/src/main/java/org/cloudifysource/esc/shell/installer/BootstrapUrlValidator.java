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
package org.cloudifysource.esc.shell.installer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.cloudifysource.shell.ShellUtils;

/****************
 * Validator for url settings in a cloud object.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class BootstrapUrlValidator {
	private static final Logger logger = Logger
			.getLogger(BootstrapUrlValidator.class.getName());
	private final Cloud cloud;

	public BootstrapUrlValidator(final Cloud cloud) {
		this.cloud = cloud;
	}

	/*****
	 * Validates the cloud object's cloudify url.
	 * 
	 * @param validationContext
	 *            the validation context.
	 * @throws CloudProvisioningException
	 *             in case of a problem with the url.
	 */
	public void validateCloudifyUrls(final ValidationContext validationContext) throws CloudProvisioningException {
		final String baseCloudifyUrl = cloud.getProvider().getCloudifyUrl();

		final SystemDefaultHttpClient client = new SystemDefaultHttpClient();

		if (baseCloudifyUrl.endsWith(".tar.gz")
				|| baseCloudifyUrl.endsWith(".zip")) {
			validateUrl(client, baseCloudifyUrl, validationContext);
		} else {
			final Set<String> scriptLanguages = getScriptLanguages();
			if (scriptLanguages.contains(ScriptLanguages.LINUX_SHELL.toString())) {
				validateUrl(client, baseCloudifyUrl + ".tar.gz", validationContext);
			}

			if (scriptLanguages.contains(ScriptLanguages.WINDOWS_BATCH.toString())) {
				validateUrl(client, baseCloudifyUrl + ".zip", validationContext);
			}
		}

	}

	private void validateUrl(final SystemDefaultHttpClient httpClient, final String cloudifyUrl,
			final ValidationContext validationContext)
			throws CloudProvisioningException {

		final HttpHead httpMethod = new HttpHead(cloudifyUrl);

		try {
			validationContext.validationOngoingEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
					ShellUtils.getFormattedMessage(CloudifyErrorMessages.EVENT_VALIDATING_CLOUDIFY_URL.getName(),
							cloudifyUrl));
			final HttpResponse response = httpClient.execute(httpMethod);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);
				logger.warning("Failed to validate Cloudify URL: " + cloudifyUrl);
				throw new CloudProvisioningException("Invalid cloudify URL: " + cloudifyUrl);
			}
			validationContext.validationEventEnd(ValidationResultType.OK);
		} catch (final ClientProtocolException e) {
			validationContext.validationOngoingEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
					" Unable to validate URL");
			validationContext.validationEventEnd(ValidationResultType.WARNING);
			logger.fine("Failed to validate Cloudify URL: " + cloudifyUrl);
		} catch (final IOException e) {
			validationContext.validationOngoingEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
					" Unable to validate URL");
			validationContext.validationEventEnd(ValidationResultType.WARNING);
			logger.fine("Failed to validate Cloudify URL: " + cloudifyUrl);
		}
	}

	private Set<String> getScriptLanguages() {
		final Set<String> langs = new HashSet<String>();

		for (final Entry<String, ComputeTemplate> entry : cloud.getCloudCompute().getTemplates().entrySet()) {
			final ComputeTemplate template = entry.getValue();
			langs.add(template.getScriptLanguage().toString());
		}

		return langs;
	}
}
