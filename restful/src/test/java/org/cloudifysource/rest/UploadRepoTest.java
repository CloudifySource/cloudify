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
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(UploadRepoTest.class.getName());

    private UploadRepo repo;
 	private static final int CLEANUP_TIMEOUT_MILLIS = 1000;
	private static final String UPLOAD_DIR_PATH =
			"src" + File.separator + "test" + File.separator + "resources" + File.separator + "upload";
	private static final String ZIP_FILE_PATH = UPLOAD_DIR_PATH + File.separator + "test.zip";
 	private static final String TEST_FILE_NAME = "test.txt";
	private static final String TXT_FILE_PATH = UPLOAD_DIR_PATH + File.separator + TEST_FILE_NAME;
    @Before
    public void init() 
    		throws RestErrorException {
        repo = new UploadRepo();
        repo.init();
		repo.setBaseDir(new File(CloudifyConstants.REST_FOLDER));
		repo.createUploadDir();
    }

    @After
    public void destroy() throws IOException {
        repo.destroy();
    }

    @Test
    public void getNotExistTest() {
        final File file = repo.get(UUID.randomUUID().toString());
        Assert.assertNull(file);
    }

    @Test
    public void testUploadTimeout() throws InterruptedException, IOException {
    	int cleanupTimeoutMillis = repo.getCleanupTimeoutMillis();
    	repo.resetTimeout(CLEANUP_TIMEOUT_MILLIS);
        try {
        	File file = new File(ZIP_FILE_PATH);
        	String dirName = null;
        	try {
        		logger.log(Level.INFO, "tring to put " + file.getAbsolutePath());
        		dirName = putTest(file);
        	} catch (RestErrorException e) {
        		fail(e.getMessage());
        	}
        	File uploadedFile = repo.get(dirName);
            logger.log(Level.FINE, "got from repo " +  ((uploadedFile == null) 
            		? "upload file [dir name = " + dirName + "] not exist in repo" : uploadedFile.getAbsolutePath()));
        	assertUploadedFile(uploadedFile);

        	// wait until the file is deleted.
        	Thread.sleep(repo.getCleanupTimeoutMillis() * 3);

        	final File restUploadDir = repo.getRestUploadDir();
        	Assert.assertNotNull(restUploadDir);
        	Assert.assertTrue(restUploadDir.isDirectory());
        	file = repo.get(dirName);
        	Assert.assertNull(file);
        } finally {
        	repo.resetTimeout(cleanupTimeoutMillis);
        }
    }

    @Test
    public void uploadZipFileTest() throws IOException {
        putAndGetTest(new File(ZIP_FILE_PATH));
    }

    @Test
    public void uploadTxtFileTest() throws IOException {
        putAndGetTest(new File(TXT_FILE_PATH));
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

    public void putAndGetTest(final File file) throws IOException {
        String uploadKey = null;
        try {
            uploadKey = putTest(file);
        } catch (RestErrorException e) {
            fail(e.getMessage());
        }
        Assert.assertNotNull(uploadKey);
        final File uploadedFile = repo.get(uploadKey);
        assertUploadedFile(uploadedFile);
    }

    private void assertUploadedFile(final File uploadedFile) throws IOException {
        Assert.assertNotNull(uploadedFile);
        // file expected to be a file and not a directory.
        Assert.assertTrue(uploadedFile.isFile());
        // unzip file if needed
        File unzippedFile = uploadedFile;
        String name = uploadedFile.getName();
        if (name.endsWith("zip")) {
            File tempDir = new File(new File(CloudifyConstants.TEMP_FOLDER), "tempDir");
            tempDir.mkdirs();
            tempDir.deleteOnExit();
            ZipUtils.unzip(uploadedFile, tempDir);
            unzippedFile = new File(tempDir, TEST_FILE_NAME);
            Assert.assertTrue(unzippedFile.exists());
            unzippedFile.deleteOnExit();
        }
        // check file name and content
        Assert.assertEquals(TEST_FILE_NAME, unzippedFile.getName());
        FileUtils.contentEquals(new File(TXT_FILE_PATH), unzippedFile);
    }
}
