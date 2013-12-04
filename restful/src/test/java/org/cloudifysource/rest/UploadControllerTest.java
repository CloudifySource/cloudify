/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.controllers.UploadController;
import org.cloudifysource.rest.repo.UploadRepo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;

import com.j_spaces.kernel.PlatformVersion;

/**
 * A test for  {@link UploadController}
 * @author yael
 *
 */
//Swap the default JUnit4 with the spring specific SpringJUnit4ClassRunner.
//This will allow spring to inject the application context
@RunWith(SpringJUnit4ClassRunner.class)
//Setup the configuration of the application context and the web mvc layer
@ContextConfiguration({"classpath:META-INF/spring/applicationContext.xml",
		"classpath:META-INF/spring/webmvc-config-upload-test.xml" })
public class UploadControllerTest extends ControllerTest {
	private static final Logger logger = Logger.getLogger(UploadControllerTest.class.getName());

    private static final String UPLOAD_RESOURCES_PATH = "src" + File.separator + "test" + File.separator
            + "resources" + File.separator + "upload";
    private static final String TEST_FILE_PATH =  UPLOAD_RESOURCES_PATH + File.separator + "test.txt";
    private static final String TEST_FILE1_PATH =  UPLOAD_RESOURCES_PATH + File.separator + "test1.txt";

    private static final String UPLOADED_FILE_NAME = "upload.zip";
    private static final String UPLOAD_URI = "/upload/" + UPLOADED_FILE_NAME;
    private String versionedUploadUri;

    private HashMap<String, HashMap<RequestMethod, HandlerMethod>> controllerMapping;
    private UploadController controller;
    private UploadRepo uploadRepo;

    private static final int TEST_UPLOAD_SIZE_LIMIT_BYTES = 10;
    private static final int TEST_CLEANUP_TIMOUT_MILLIS = 3000;


    @Before
    public void init() throws NoSuchMethodException, RestErrorException {
        String version = PlatformVersion.getVersion();
        versionedUploadUri = "/" + version + UPLOAD_URI;
        controller = applicationContext.getBean(UploadController.class);
        uploadRepo = applicationContext.getBean(UploadRepo.class);
        uploadRepo.init();
        uploadRepo.setBaseDir(new File(CloudifyConstants.REST_FOLDER));
        uploadRepo.createUploadDir();
        controllerMapping = new HashMap<String, HashMap<RequestMethod, HandlerMethod>>();
        HashMap<RequestMethod, HandlerMethod> map = new HashMap<RequestMethod, HandlerMethod>();
        HandlerMethod method = new HandlerMethod(controller, "upload", String.class, MultipartFile.class);
        map.put(RequestMethod.POST, method);
        controllerMapping.put(versionedUploadUri, map);
    }

    @Override
    public HandlerMethod getExpectedMethod(final String requestUri, final RequestMethod requestMethod) {
        HashMap<RequestMethod, HandlerMethod> hashMap = controllerMapping.get(requestUri);
        Assert.assertNotNull(hashMap);
        return hashMap.get(requestMethod);
    }

    private UploadResponse uploadFile(final File file, final String testName)
    		throws Exception {
    	logger.log(Level.FINE, 
    			"[" + testName + "] - trying to upload file " + file.getName() 
        		+ ", repo upload size limit in bytes: " + uploadRepo.getUploadSizeLimitBytes() 
        		+ ", repo cleanup timeout in millis: " + uploadRepo.getCleanupTimeoutMillis());
        MockHttpServletResponse response;
        try {
        	response = testPostFile(versionedUploadUri, file);
        } catch (Exception e) {
        	logger.log(Level.WARNING, "upload response thrown an exception: " + e.getMessage());
        	throw e;
        }
    	logger.log(Level.FINE, "upload response's content: " + response.getContentAsString());
        ObjectMapper objectMapper = new ObjectMapper();
        Response<UploadResponse> readValue = objectMapper.readValue(response.getContentAsString(),
                new TypeReference<Response<UploadResponse>>() { });
        UploadResponse uploadResponse = readValue.getResponse();
        return uploadResponse;
    }

    @Test
    public void testUpload() throws Exception {
        File file = new File(TEST_FILE_PATH);
        UploadResponse uploadResponse = uploadFile(file, "testUpload");
        String uploadKey = uploadResponse.getUploadKey();
    	logger.log(Level.FINE, "file has been uploaded. the upload key is " + uploadKey);
        Assert.assertNotNull(uploadKey);
        assertUploadedFileExists(file, uploadKey);
    }

    @Test
    public void testUploadDifferentName() throws Exception {
        File file = new File(TEST_FILE1_PATH);
        UploadResponse uploadResponse = uploadFile(file, "testUploadDifferentName");
        String uploadKey = uploadResponse.getUploadKey();
        Assert.assertNotNull(uploadKey);
        assertUploadedFileExists(file, uploadKey);
    }

    @Test
    public void testUploadExceededSizeLimitFile() throws Exception {
    	logger.log(Level.FINE, "set the upload size limit to " + TEST_UPLOAD_SIZE_LIMIT_BYTES + " bytes.");
        uploadRepo.setUploadSizeLimitBytes(TEST_UPLOAD_SIZE_LIMIT_BYTES);
        File uploadFile = new File(TEST_FILE_PATH);
        MockHttpServletResponse response = null;
        long fileSize = uploadFile.length();
    	logger.log(Level.FINE, 
    			"trying to upload file of size " + fileSize 
        		+ " expecting " + CloudifyMessageKeys.UPLOAD_FILE_SIZE_LIMIT_EXCEEDED.getName());
        try {
            response = testPostFile(versionedUploadUri, uploadFile);
            Assert.fail("Tring to upload a file of zise " + fileSize + "expected to failed. response "
                    + response.getContentAsString());
        } catch (RestErrorException e) {
            Map<String, Object> errorDescription = e.getErrorDescription();
            String status = (String) errorDescription.get("status");
            Assert.assertEquals("error", status);
            String errorMsg = (String) errorDescription.get("error");
            Assert.assertEquals(CloudifyMessageKeys.UPLOAD_FILE_SIZE_LIMIT_EXCEEDED.getName(), errorMsg);
            Object[] args = (Object[]) errorDescription.get("error_args");
            Object[] expectedArgs = {UPLOADED_FILE_NAME, fileSize, uploadRepo.getUploadSizeLimitBytes()};
            Assert.assertArrayEquals(expectedArgs, args);
        }  finally {
        	logger.log(Level.FINE, 
        			"setting the upload size limit back to "
        			+ CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES + " bytes.");
            uploadRepo.setUploadSizeLimitBytes(CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES);
        }
    }

    @Test
    public void testUploadTimeout() throws Exception {
    	int cleanupTimeoutMillis = uploadRepo.getCleanupTimeoutMillis();
        logger.log(Level.FINE, "setting the cleanup timeout " + TEST_CLEANUP_TIMOUT_MILLIS + " millis.");
        uploadRepo.resetTimeout(TEST_CLEANUP_TIMOUT_MILLIS);
        try {       	
        	File file = new File(TEST_FILE_PATH);
        	UploadResponse uploadResponse = uploadFile(file, "testUploadTimeout");
        	String uploadKey = uploadResponse.getUploadKey();
        	logger.log(Level.FINE, "successfully uploaded file " + file.getName() + " upload key is " + uploadKey);
        	Assert.assertNotNull(uploadKey);
        	File uploadedFile = assertUploadedFileExists(file, uploadKey);
        	String parentPath = uploadedFile.getParentFile().getAbsolutePath();
            logger.log(Level.INFO, 
            		"sleeping for " + (TEST_CLEANUP_TIMOUT_MILLIS * 3) / DateUtils.MILLIS_PER_SECOND + " seconds");
        	Thread.sleep(TEST_CLEANUP_TIMOUT_MILLIS * 3);
        	File expectedToBeDeletedFolder = new File(parentPath);
        	logger.log(Level.FINE, "validate that the folder [" 
        			+ expectedToBeDeletedFolder.getAbsolutePath() + "] was deleted:");
        	boolean exists = expectedToBeDeletedFolder.exists();
        	logger.log(Level.FINE, "The folder [" + expectedToBeDeletedFolder.getAbsolutePath() 
        			+ "] (expected to be deleted at this point) " + (exists ? " exists" : " not exists."));
			Assert.assertFalse(exists);
        } finally {
        	logger.log(Level.FINE, "setting the cleanup timeout back to " + cleanupTimeoutMillis + " millis");
        	uploadRepo.resetTimeout(cleanupTimeoutMillis);
        }
    }

    @Test
    public void testUplaodFileNotExist() {
        File file = new File("notExist.zip");
        try {
            uploadFile(file, "testUplaodFileNotExist");
            Assert.fail("[testUplaodFileNotExist] - FileNotFoundException expected");
        } catch (FileNotFoundException e) {
        	logger.log(Level.FINE, "cought exeption " + e.getMessage() + " as expected");
        } catch (Exception e) {
            Assert.fail("cought exception other than FileNotFoundException [" 
            		+ e.getClass() + "] message: " + e.getMessage());
		}

    }

    private File assertUploadedFileExists(final File expectedFile, final String uploadKey)
            throws IOException {
        File restTempDir = new File(CloudifyConstants.REST_FOLDER);
        File uploadsFolder = new File(restTempDir, CloudifyConstants.UPLOADS_FOLDER_NAME);
        File uploadedFileDir = new File(uploadsFolder, uploadKey);
        logger.log(Level.FINE, "uploaded file's folder: " + uploadedFileDir.getAbsolutePath());
        Assert.assertNotNull(uploadedFileDir);
        logger.log(Level.FINE, "uploaded file's folder exists: " + uploadedFileDir.exists());
        Assert.assertTrue(uploadedFileDir.exists());
        logger.log(Level.FINE, "uploaded file's folder isDirectory: " + uploadedFileDir.isDirectory());
        Assert.assertTrue(uploadedFileDir.isDirectory());
        File uploadedFile = new File(uploadedFileDir, UPLOADED_FILE_NAME);
        logger.log(Level.FINE, "uploaded file " + uploadedFile.getAbsolutePath() 
        		+ (uploadedFile.exists() ? "" : " not") + " exists.");
        Assert.assertTrue(uploadedFile.exists());
        logger.log(Level.FINE, "uploaded file isFile: " + uploadedFile.isFile());
        Assert.assertTrue(uploadedFile.isFile());
        boolean contentEquals = FileUtils.contentEquals(expectedFile, uploadedFile);
        logger.log(Level.FINE, 
        		"uploaded file [" + uploadedFile.getAbsolutePath() 
        		+ "] content is " + (contentEquals ? "" : "not") 
        		+ " equal to the expected content [ of file - " 
        		+ expectedFile.getAbsolutePath() + "]");
		Assert.assertTrue(contentEquals);
        return uploadedFile;
    }

}
