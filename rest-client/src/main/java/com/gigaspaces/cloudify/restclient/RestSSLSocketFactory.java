package com.gigaspaces.cloudify.restclient;

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
import java.security.cert.X509Certificate;

public class RestSSLSocketFactory extends SSLSocketFactory {

	/**
	 * SSL context (using algorithm TLS).
	 */
	private SSLContext sslContext = SSLContext.getInstance(TLS);

	/**
	 * Ctor.
	 * 
	 * @param truststore
	 *            a {@link KeyStore} containing one or several trusted
	 *            certificates to enable enable server authentication.
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
	public RestSSLSocketFactory(final KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super(truststore);

		TrustManager tm = new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(final X509Certificate[] chain,
					final String authType)
					throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub

			}

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
