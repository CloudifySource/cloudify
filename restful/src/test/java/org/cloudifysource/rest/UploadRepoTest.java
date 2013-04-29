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
 ******************************************************************************/
package org.cloudifysource.rest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.repo.UploadRepo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;


/**
 * 
 * @author yael
 * 
 */
public class UploadRepoTest {

	private UploadRepo repo;
	private static final int CLEANUP_TIMEOUT_SECONDS = 3;
	private static final String TEST_FILE_NAME = "test.txt";
	private static final String TEST_FILE_PATH = "src/test/resources/upload/test.zip";
	private static final String TXT_EXTENSION_TEST_FILE_PATH = "src/test/resources/upload/" + TEST_FILE_NAME;
	private static final String RAR_EXTENSION_TEST_FILE_PATH = "src/test/resources/upload/test.rar";
	
	@Before
	public void init() throws IOException {
		repo = new UploadRepo();
		repo.init();
	}

	@After
	public void destroy() throws IOException {
		repo.destroy();
	}

	public static MultipartFile createNewMultiFile(final File file) throws IOException {
		byte[] content = FileUtils.readFileToByteArray(file);
		final MockMultipartFile mockMultipartFile = new MockMultipartFile(
				CloudifyConstants.UPLOAD_FILE_PARAM_NAME, file.getName(), "text/plain", content);
		return mockMultipartFile;
	}

	private String putTest(final File testFile) throws IOException, RestErrorException {
		final MultipartFile multiFile = createNewMultiFile(testFile);
		String dirName = null;
		try {
			dirName = repo.put(null, multiFile);
		} catch (final IOException e) {
			fail(e.getMessage());
		}
		Assert.assertNotNull(dirName);
		return dirName;
	}

	private void assertUploadedFile(final File expectedFile, final File uploadedFile) throws IOException {
		Assert.assertNotNull(uploadedFile);
		// file expected to be a file and not a directory.
		Assert.assertTrue(uploadedFile.isFile());
		// unzip file
		Assert.assertTrue(uploadedFile.getName().endsWith(CloudifyConstants.PERMITTED_EXTENSION));
		File tempDir = new File(new File(CloudifyConstants.TEMP_FOLDER), "tempDir");
		tempDir.mkdirs();
		tempDir.deleteOnExit();
		File zipCopyFile = File.createTempFile("test", ".zip", tempDir);
		FileUtils.copyFile(uploadedFile, zipCopyFile);
		ZipUtils.unzip(zipCopyFile, tempDir);
		File unzippedFile = new File(tempDir, TEST_FILE_NAME);
		unzippedFile.deleteOnExit();
		// check file name and content
		Assert.assertEquals(expectedFile.getName(), unzippedFile.getName());
		FileUtils.contentEquals(expectedFile, unzippedFile);
		ZipFile zipFile = new ZipFile(zipCopyFile);
		zipFile.close();
	}
	
	
	@Test
	public void putAndGetTest() throws IOException {
		File file = new File(TEST_FILE_PATH);
		String dirName = null;
		try {
			dirName = putTest(file);
		} catch (RestErrorException e) {
			fail(e.getMessage());
		}
		Assert.assertNotNull(dirName);
		final File uploadedFile = repo.get(dirName);
		assertUploadedFile(new File(TXT_EXTENSION_TEST_FILE_PATH), uploadedFile);
	}

	@Test
	public void getNotExistTest() {
		final File file = repo.get(UUID.randomUUID().toString());
		Assert.assertNull(file);
	}

	@Test
	public void testtest() {
	}
	
	@Test
	public void getTimoutedFile() throws InterruptedException, IOException {
		repo.resetTimeout(CLEANUP_TIMEOUT_SECONDS);
		File file = new File(TEST_FILE_PATH);
		String dirName = null;
		try {
			dirName = putTest(file);
		} catch (RestErrorException e) {
			fail(e.getMessage());
		}
		File uploadedFile = repo.get(dirName);
		assertUploadedFile(new File(TXT_EXTENSION_TEST_FILE_PATH), uploadedFile);
		
		// wait until the file is deleted.
		Thread.sleep(repo.getCleanupTimeoutSeconds() * 2000);

		final File restUploadDir = repo.getRestUploadDir();
		Assert.assertNotNull(restUploadDir);
		Assert.assertTrue(restUploadDir.isDirectory());
		file = repo.get(dirName);
		Assert.assertNull(file);
	}
	
	@Test
	public void wrongFileExtension() throws IOException {
		try {
			final String dirName = putTest(new File(TXT_EXTENSION_TEST_FILE_PATH));
			fail("Wrong file extension (" + TXT_EXTENSION_TEST_FILE_PATH + "), expected RestErrorException. " +
					"Uploaded file's folder name is " + dirName);
		} catch (RestErrorException e) {
			
		}
		try {
			final String dirName = putTest(new File(RAR_EXTENSION_TEST_FILE_PATH));
			fail("Wrong file extension (" + RAR_EXTENSION_TEST_FILE_PATH + "), expected RestErrorException. " +
					"Uploaded file's folder name is " + dirName);
		} catch (RestErrorException e) {
			
		}

	}
}
