/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * 
 * @author elip
 * 
 */
public class MicrosoftAzureSSLHelper {

	// Key store constants
	private static final String SUN_X_509_ALGORITHM = "SunX509";
	private static final String KEY_STORE_CONTEXT = "PKCS12";

	private String pathToPfxFile;
	private String pfxPassword;

	public MicrosoftAzureSSLHelper(final String pathToPfx, final String pfxPassword) {
		this.pathToPfxFile = pathToPfx;
		this.pfxPassword = pfxPassword;
	}

	/**
	 * 
	 * @return .
	 * @throws NoSuchAlgorithmException .
	 * @throws KeyStoreException .
	 * @throws CertificateException .
	 * @throws IOException .
	 * @throws UnrecoverableKeyException .
	 * @throws KeyManagementException .
	 */
	public SSLContext createSSLContext() throws NoSuchAlgorithmException,
			KeyStoreException, CertificateException, IOException,
			UnrecoverableKeyException, KeyManagementException {

		SSLContext context = null;
		InputStream pfxFile = new FileInputStream(new File(pathToPfxFile));

		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(SUN_X_509_ALGORITHM);
		KeyStore keyStore = KeyStore.getInstance(KEY_STORE_CONTEXT);

		keyStore.load(pfxFile, pfxPassword.toCharArray());
		pfxFile.close();

		keyManagerFactory.init(keyStore, pfxPassword.toCharArray());

		context = SSLContext.getInstance("SSL");
		context.init(keyManagerFactory.getKeyManagers(), null,
				new SecureRandom());

		return context;
	}
}
