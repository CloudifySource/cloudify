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
package org.cloudifysource.dsl.download;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.tools.download.ChecksumVerifierException;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloadException;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloadFacade;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloadFacadeImpl;
import org.cloudifysource.dsl.internal.tools.download.ResourceDownloader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test starts an embedded Jetty server on the localhost
 *  and tests download requests made by the download facade utility.
 *  
 * @author adaml
 * @since 2.6.0
 */
public class ResourceDownloaderTest {

	//default resource params.
	private static final String RESOURCE_NAME = "testResource.txt";
	private static final String DESTINATION_FOLDER = "src/test/resources/resourceDownloader/downloadFolder/";
	private static final String RESOURCE_DESTINATION = DESTINATION_FOLDER + RESOURCE_NAME;
	private static final String RESOURCE_URL = "http://localhost:8080/" + RESOURCE_NAME;
	private static final String RESOURCE_FOLDER = "src/test/resources/resourceDownloader/";
	
	//used for testing checksum failure.
	private static final String DUMMY_RESOURCE_NAME = "dummyTestResource.txt";
	private static final String DUMMY_RESOURCE_URL = "http://localhost:8080/" + DUMMY_RESOURCE_NAME;
	private static final ResourceDownloadFacade rdf = 
			new ResourceDownloadFacadeImpl(new ResourceDownloader());
	
	private static final Server server = new Server(8080);
	
	@BeforeClass
	public static void beforeClass() 
			throws Exception {
		
		//Start embedded jetty server.
		final URL resourceUrl = new File(RESOURCE_FOLDER).
							getAbsoluteFile().toURI().toURL();
		final Resource resource = new FileResource(resourceUrl);
		final ResourceHandler handler = new ResourceHandler();
		handler.setBaseResource(resource);
		server.setHandler(handler);
		server.start();
	}
	
	@AfterClass
	public static void afterClass() 
			throws Exception {
		server.stop();
	}

	@Test
	public void testTimeout() {
		//Try doGet with very short timeout.
		try {
			rdf.get(RESOURCE_URL, RESOURCE_DESTINATION, false, 1, TimeUnit.MICROSECONDS);
			Assert.fail("Expected timeout exception.");
		} catch (TimeoutException e) {
			//test passed.
		} catch (ResourceDownloadException e) {
			Assert.fail("Expecting timeout exception. got " + e.getMessage());
		}
	}
	
	@Test
	public void testBadChecksumVerification() throws Exception {
		//run doGet on a dummy file and try to verify it against
		//non-matching checksum files.
		assertChecksumFailure(RESOURCE_URL + ".md5");
		cleanDownloadFolder();
		assertChecksumFailure(RESOURCE_URL + ".sha1");
		cleanDownloadFolder();
		assertChecksumFailure(RESOURCE_URL + ".sha256");
		cleanDownloadFolder();
		assertChecksumFailure(RESOURCE_URL + ".sha384");
		cleanDownloadFolder();
		assertChecksumFailure(RESOURCE_URL + ".sha512");
		cleanDownloadFolder();
	}

	void assertChecksumFailure(final String hashUrl) {
		try {
			rdf.get(DUMMY_RESOURCE_URL, RESOURCE_DESTINATION, 
					false, hashUrl);
			Assert.fail("File checksum verified. This is not suppose to happen.");
		} catch (Exception e) {
			if (!(e.getCause() instanceof ChecksumVerifierException)) {
				Assert.fail("expecting failure due to checksum validation");
			}
		}
	}
	
	@Test
	public void testResourceDownload() throws Exception {
		//test get with checksum verification. 
		//Hashing algorithm determined by the checksum file extension.
		rdf.get(RESOURCE_URL, 
				RESOURCE_DESTINATION);
		cleanDownloadFolder();
		assertDownloadSuccess(RESOURCE_URL + ".md5");
		cleanDownloadFolder();
		assertDownloadSuccess(RESOURCE_URL + ".sha1");
		cleanDownloadFolder();
		assertDownloadSuccess(RESOURCE_URL + ".sha256");
		cleanDownloadFolder();
		assertDownloadSuccess(RESOURCE_URL + ".sha384");
		cleanDownloadFolder();
		assertDownloadSuccess(RESOURCE_URL + ".sha512");
		cleanDownloadFolder();
	}
	
	@Test
	public void testChecksumFormatting() throws Exception {
		//Tests checksum verification on checksum files that 
		//are formatted using format type '{0}'.
		assertFormattedChecksumVerification("testResourceFormatted.txt.md5");
		cleanDownloadFolder();
		assertFormattedChecksumVerification("testResourceFormatted.txt.sha1");
		cleanDownloadFolder();
		assertFormattedChecksumVerification("testResourceFormatted.txt.sha256");
		cleanDownloadFolder();
		assertFormattedChecksumVerification("testResourceFormatted.txt.sha384");
		cleanDownloadFolder();
		assertFormattedChecksumVerification("testResourceFormatted.txt.sha512");
		cleanDownloadFolder();
	}
	
	@Test
	public void testSkipExistingFlag() throws Exception {
		//asserts file was not overridden.
		rdf.get(RESOURCE_URL, 
				RESOURCE_DESTINATION, 
				false, 
				RESOURCE_URL + ".md5");
		
		File downloadedResource = new File(DESTINATION_FOLDER, RESOURCE_NAME);
		long lastModified = downloadedResource.lastModified();
		
		rdf.get(RESOURCE_URL, 
				RESOURCE_DESTINATION, 
				true, 
				RESOURCE_URL + ".md5");
		Assert.assertTrue("File was modified but skip existing flag is true", 
						downloadedResource.lastModified() == lastModified);
		cleanDownloadFolder();
	}

	void assertFormattedChecksumVerification(final String hashFileName)
			throws ResourceDownloadException {
		
		//test verification for special checksum format types
		final ResourceDownloader downloader = new ResourceDownloader();
		final File resourceFile = new File(RESOURCE_FOLDER, RESOURCE_NAME);
		final File hashFile = new File(RESOURCE_FOLDER, hashFileName);
		
		downloader.setFormat(new MessageFormat("{0}"));
		downloader.setDestFile(resourceFile);
		//Expecting not to throw exception.
		downloader.verifyResourceChecksum(hashFile);
	}

	void assertDownloadSuccess(final String hashUrl) throws ResourceDownloadException,
			TimeoutException, IOException {
		rdf.get(RESOURCE_URL, 
				RESOURCE_DESTINATION, 
				false, 
				hashUrl);
		assertFilesDownloaded("testResource.txt", getResourceName(hashUrl));
	}
	
	private String getResourceName(final String url) {
		final int slashIndex = url.lastIndexOf('/');
		final String filename = url.substring(slashIndex + 1);
		return filename;
	}

	private void assertFilesDownloaded(final String resourceFileName, final String hashFileName) throws IOException {
		
		final File downloadedResourceFile = new File(DESTINATION_FOLDER, resourceFileName);
		final File downloadedHashFile = new File(DESTINATION_FOLDER, hashFileName);
		Assert.assertTrue("Resource file download failed", downloadedResourceFile.exists());
		Assert.assertTrue("Hash file download failed", downloadedHashFile.exists());
		
		final File originalResourceFile = new File(RESOURCE_FOLDER, resourceFileName);
		final File originalHashFile = new File(RESOURCE_FOLDER, hashFileName);
		//asserting file content is the same.
		Assert.assertTrue("Resource file was not downloaded properly",
				FileUtils.readFileToString(downloadedResourceFile).
				equals(FileUtils.readFileToString(originalResourceFile)));
		Assert.assertTrue("Hash file was not downloaded properly",
				FileUtils.readFileToString(downloadedHashFile).
				equals(FileUtils.readFileToString(originalHashFile)));
		
	}

	private void cleanDownloadFolder() throws IOException {
		FileUtils.cleanDirectory(new File(DESTINATION_FOLDER));
	}
}
