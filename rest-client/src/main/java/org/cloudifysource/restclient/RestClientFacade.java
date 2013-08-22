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
package org.cloudifysource.restclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.restclient.exceptions.RestClientException;

/********************
 * The Rest Client Facade is a wrapper around the basic RestClient class. It wraps commonly used functionality in one
 * place. Most of the methods available here will actually invoke multiple REST API calls on the underlying RestClient
 * object.
 * 
 * Note that the rest client object should already be connected to the rest server.
 * 
 * @author barakme
 * @since 2.7.0
 */
public class RestClientFacade {

	private static final int BUFFER_SIZE = 1024;

	private final RestClient client;

	/*********
	 * Constructor. Initializes the facade with a connected instance of the basic rest client.
	 * 
	 * @param client
	 */
	public RestClientFacade(final RestClient client) {
		super();
		this.client = client;
	}

	public RestClient getClient() {
		return client;
	}

	/*********
	 * Adds a directory of templates to the cloud configuration.
	 * 
	 * @param directory
	 *            the directory where the template files are placed.
	 * @return the REST API response.
	 * @throws RestClientException
	 *             If a REST API call failed.
	 * @throws IOException
	 *             if There was a problem while creating a ZIP file of the templates directory.
	 * @throws AddTemplatesException  .
	 */
	public AddTemplatesResponse addTemplates(final File directory) throws RestClientException, IOException, AddTemplatesException  {

		if (directory == null) {
			throw new IllegalArgumentException("directory can't be null");
		}

		if (!directory.exists() || !directory.isDirectory()) {
			throw new IllegalArgumentException("Expected a directory at: " + directory.getAbsolutePath());
		}

		final File zipFile = zip(directory);
		try {

			UploadResponse uploadResponse = null;
			uploadResponse = client.upload(null, zipFile);

			final AddTemplatesRequest request = new AddTemplatesRequest();
			request.setUploadKey(uploadResponse.getUploadKey());
			AddTemplatesResponse addTemplatesResponse = null;
			addTemplatesResponse = client.addTemplates(request);
			return addTemplatesResponse;
		} finally {
			zipFile.delete();
		}

	}

	/***********
	 * Zips a directory into a temp file. The temp file is created and marked for deletion.
	 * 
	 * @param directory
	 *            the directory to zip.
	 * @return the zip file.
	 * @throws IOException
	 *             in case of an error.
	 */
	private File zip(final File directory)
			throws IOException {
		final File zipFile = File.createTempFile(directory.getName(), ".zip");
		zipFile.deleteOnExit();
		final URI base = directory.toURI();
		final File toZip = new File(zipFile, "");
		toZip.setWritable(true);
		final Stack<File> stack = new Stack<File>();
		stack.push(directory);
		final OutputStream out = new FileOutputStream(toZip);

		final ZipOutputStream zout = new ZipOutputStream(out);

		try {
			while (!stack.isEmpty()) {
				final File currentDirectory = stack.pop();
				for (final File kid : currentDirectory.listFiles()) {
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory()) {
						stack.push(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					} else {
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		} finally {
			zout.close();
		}

		return zipFile;
	}

	private static void copy(final InputStream in, final OutputStream out)
			throws IOException {
		final byte[] buffer = new byte[BUFFER_SIZE];
		while (true) {
			final int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(final File file, final OutputStream out)
			throws IOException {
		final InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

}
