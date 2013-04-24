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

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
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
	private static final String TEST_FILE_PATH = "src/test/resources/upload/test.txt";
	
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

	private String putTest(final File testFile) throws IOException {
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
		// check file name and content
		Assert.assertEquals(expectedFile.getName(), uploadedFile.getName());
		FileUtils.contentEquals(expectedFile, uploadedFile);
	}

	@Test
	public void putAndGetTest() throws IOException {
		final byte[] testFileContent = { 1, 2, 3 };
		File file = new File("testFile");
		file.deleteOnExit();
		FileUtils.writeByteArrayToFile(file, testFileContent);
		final String dirName = putTest(file);
		Assert.assertNotNull(dirName);
		final File uploadedFile = repo.get(dirName);
		assertUploadedFile(file, uploadedFile);
	}

	@Test
	public void getNotExistTest() {
		final File file = repo.get(UUID.randomUUID().toString());
		Assert.assertNull(file);
	}

	@Test
	public void getTimoutedFile() throws InterruptedException, IOException {
		repo.resetTimeout(CLEANUP_TIMEOUT_SECONDS);
		File file = new File(TEST_FILE_PATH);
		final String dirName = putTest(file);
		File uploadedFile = repo.get(dirName);
		assertUploadedFile(file, uploadedFile);
		
		// wait until the file is deleted.
		Thread.sleep(repo.getCleanupTimeoutSeconds() * 2000);

		final File restUploadDir = repo.getRestUploadDir();
		Assert.assertNotNull(restUploadDir);
		Assert.assertTrue(restUploadDir.isDirectory());
		file = repo.get(dirName);
		Assert.assertNull(file);
	}
}
