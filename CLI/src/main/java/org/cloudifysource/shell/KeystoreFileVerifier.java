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
package org.cloudifysource.shell;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/***********
 * Wrapper for code that verifies that a keystore file is valid and can be
 * opened with a given password.
 * 
 * @author barakme
 * @since 2.3.0
 */
public class KeystoreFileVerifier {

	/******
	 * Checks that a keystore file is valid and can be decrypted using the given
	 * password.
	 * 
	 * @param keystoreFile
	 *            the keystore file.
	 * @param password
	 *            the password.
	 * @throws CLIStatusException
	 *             if the keystore file could not be opened.
	 */
	public void verifyKeystoreFile(final File keystoreFile, final String password) throws CLIStatusException {
		try {
			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			// get user password and file input stream
			final char[] passwordBuffer = password.toCharArray();
			final java.io.FileInputStream fis = new java.io.FileInputStream(keystoreFile);
			ks.load(fis, passwordBuffer);
			fis.close();
		} catch (final IOException e) {
			throw new CLIStatusException(e, CloudifyErrorMessages.INVALID_KEYSTORE_FILE.getName(), e.getMessage());
		} catch (final KeyStoreException e) {
			throw new CLIStatusException(e, CloudifyErrorMessages.INVALID_KEYSTORE_FILE.getName(), e.getMessage());
		} catch (final NoSuchAlgorithmException e) {
			throw new CLIStatusException(e, CloudifyErrorMessages.INVALID_KEYSTORE_FILE.getName(), e.getMessage());
		} catch (final CertificateException e) {
			throw new CLIStatusException(e, CloudifyErrorMessages.INVALID_KEYSTORE_FILE.getName(), e.getMessage());
		}
	}
}
