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

package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.domain.EncryptedPasswordAndPrivateKey;
import org.jclouds.cloudstack.functions.WindowsLoginCredentialsFromEncryptedData;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.crypto.Crypto;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.PasswordData;
import org.jclouds.ec2.features.WindowsApi;
import org.jclouds.encryption.internal.JCECrypto;

/************
 * Handles decryption of encrypted Administrator passwords for Amazon EC2 windows instances.
 *
 * @author barakme
 * @since 2.1.0
 *
 */
public class EC2WindowsPasswordHandler {

	private static final int PASSWORD_POLLING_INTERVAL_MILLIS = 5000;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(EC2WindowsPasswordHandler.class.getName());

	/************
	 * Returns the decrypted password.
	 *
	 * @param node
	 *            the compute node.
	 * @param context
	 *            the compute context.
	 * @param end
	 *            the operation end time.
	 * @param pemFile
	 *            the private key file used to decrypt the password.
	 * @return the decrypted password.
	 * @throws InterruptedException .
	 * @throws TimeoutException .
	 * @throws CloudProvisioningException .
	 */
	public LoginCredentials getPassword(final NodeMetadata node, final ComputeServiceContext context, final long end,
			final File pemFile)
			throws InterruptedException, TimeoutException, CloudProvisioningException {

		final Location zone = node.getLocation();
		final Location region = zone.getParent();
		WindowsApi winApi = EC2Client.class.cast(context.unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi())
		.getWindowsApiForRegion(region.getId()).get();
		final String id = node.getId();

		String key;
		try {
			key = FileUtils.readFileToString(pemFile);
		} catch (final IOException e) {
			throw new CloudProvisioningException("Failed to read key file: " + pemFile, e);
		}

		final String amiId = id.split("/")[1];
		while (System.currentTimeMillis() < end) {
			logger.fine("Reading Windows password");

			final PasswordData passwordData = winApi.getPasswordDataInRegion(region.getId(), amiId);

			if (passwordData == null || passwordData.getPasswordData() == null
					|| passwordData.getPasswordData().isEmpty()) {
				Thread.sleep(PASSWORD_POLLING_INTERVAL_MILLIS);
			} else {
				final String encryptedPassword = passwordData.getPasswordData();

				LoginCredentials credentials;
				try {
					credentials = decryptPasswordData(
							encryptedPassword, key);
				} catch (final NoSuchAlgorithmException e) {
					throw new CloudProvisioningException("Failed to decrypt windows password: " + e.getMessage(), e);
				} catch (final CertificateException e) {
					throw new CloudProvisioningException("Failed to decrypt windows password: " + e.getMessage(), e);
				} catch (final IOException e) {
					throw new CloudProvisioningException("Failed to decrypt windows password: " + e.getMessage(), e);
				}
				return credentials;

			}
		}

		throw new TimeoutException("Failed to retrieve EC2 Windows password in the allocated time");

	}

	private LoginCredentials decryptPasswordData(final String encryptedPassword, final String key)
			throws NoSuchAlgorithmException, CertificateException, IOException {

		final Crypto crypto = new JCECrypto();

		final WindowsLoginCredentialsFromEncryptedData f = new WindowsLoginCredentialsFromEncryptedData(crypto);

		final LoginCredentials credentials = f.apply(EncryptedPasswordAndPrivateKey.builder().encryptedPassword(encryptedPassword).privateKey(key).build());

		return credentials;

	}
}