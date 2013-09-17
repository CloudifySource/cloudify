/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.restclient;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import java.security.cert.X509Certificate;

/**
 * Creates a SSL socket for the REST communication.
 */
public class RestSSLSocketFactory extends SSLSocketFactory {

	private SSLContext sslContext = SSLContext.getInstance(TLS);

	/**
	 * Ctor.
	 * 
	 * @param truststore
	 *            a {@link KeyStore} containing one or several trusted
	 *            certificates to enable server authentication.
	 * @throws NoSuchAlgorithmException
	 *             Reporting failure to create SSLSocketFactory with the given
	 *             trust-store and algorithm TLS or initialize the SSLContext.
	 * @throws KeyManagementException
	 *             Reporting failure to create SSLSocketFactory with the given
	 *             trust-store and algorithm TLS or initialize the SSLContext.
	 * @throws KeyStoreException
	 *             Reporting failure to create SSLSocketFactory with the given
	 *             trust-store and algorithm TLS or initialize the SSLContext.
	 * @throws UnrecoverableKeyException
	 *             Reporting failure to create SSLSocketFactory with the given
	 *             trust-store and algorithm TLS or initialize the SSLContext.
	 */
	public RestSSLSocketFactory(final KeyStore trustStore) 
			throws KeyManagementException, UnrecoverableKeyException, 
			NoSuchAlgorithmException, KeyStoreException {
		this(trustStore, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}
	public RestSSLSocketFactory(final KeyStore trustStore, final X509HostnameVerifier hostnameVarifier)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super(null, null, null, trustStore, null, hostnameVarifier);

		TrustManager tm = new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(final X509Certificate[] chain,
					final String authType)
					throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkServerTrusted(final X509Certificate[] chain,
					final String authType)
					throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub

			}
		};

		sslContext.init(null, new TrustManager[]{tm}, null);
	}

	@Override
	public final Socket createSocket(final Socket socket, final String host,
			final int port, final boolean autoClose) throws IOException {
		return sslContext.getSocketFactory().createSocket(socket, host, port,
				autoClose);
	}

	@Override
	public final Socket createSocket() throws IOException {
		return sslContext.getSocketFactory().createSocket();
	}
}
