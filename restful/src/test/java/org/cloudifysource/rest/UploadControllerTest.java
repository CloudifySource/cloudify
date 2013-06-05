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

import org.apache.commons.io.FileUtils;
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
import org.junit.Ignore;
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
    private static final int TEST_CLEANUP_TIMOUT_MILLIS = 100;


    @Before
    public void init() throws NoSuchMethodException, IOException {
        String version = PlatformVersion.getVersion();
        versionedUploadUri = "/" + version + UPLOAD_URI;
        controller = applicationContext.getBean(UploadController.class);
        uploadRepo = applicationContext.getBean(UploadRepo.class);
        uploadRepo.resetTimeout(TEST_CLEANUP_TIMOUT_MILLIS);
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

    private UploadResponse uploadFile(final File file) throws Exception {
        MockHttpServletResponse response = testPostFile(versionedUploadUri, file);
        ObjectMapper objectMapper = new ObjectMapper();
        Response<UploadResponse> readValue = objectMapper.readValue(response.getContentAsString(),
                new TypeReference<Response<UploadResponse>>() { });
        UploadResponse uploadResponse = readValue.getResponse();
        return uploadResponse;
    }

    @Test
    @Ignore
    public void testUpload() throws Exception {
        File file = new File(TEST_FILE_PATH);
        UploadResponse uploadResponse = uploadFile(file);
        String uploadKey = uploadResponse.getUploadKey();
        Assert.assertNotNull(uploadKey);
        assertUploadedFileExists(file, uploadKey);
    }

    @Test
    @Ignore
    public void testUploadDifferentName() throws Exception {
        File file = new File(TEST_FILE1_PATH);
        UploadResponse uploadResponse = uploadFile(file);
        String uploadKey = uploadResponse.getUploadKey();
        Assert.assertNotNull(uploadKey);
        assertUploadedFileExists(file, uploadKey);
    }

    @Test
    @Ignore
    public void testUploadExceededSizeLimitFile() throws Exception {
        uploadRepo.setUploadSizeLimitBytes(TEST_UPLOAD_SIZE_LIMIT_BYTES);
        File uploadFile = new File(TEST_FILE_PATH);
        MockHttpServletResponse response = null;
        long fileSize = uploadFile.length();
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
            uploadRepo.setUploadSizeLimitBytes(CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES);
        }
    }

    @Test
    @Ignore
    public void testUploadTimeout() throws Exception {
        File file = new File(TEST_FILE_PATH);
        UploadResponse uploadResponse = uploadFile(file);
        String uploadKey = uploadResponse.getUploadKey();
        Assert.assertNotNull(uploadKey);
        File uploadedFile = assertUploadedFileExists(file, uploadKey);
        String parentPath = uploadedFile.getParentFile().getAbsolutePath();

        Thread.sleep(TEST_CLEANUP_TIMOUT_MILLIS * 3);

        File expectedToBeDeletedFolder = new File(parentPath);
        Assert.assertFalse(expectedToBeDeletedFolder.exists());
    }

    @Test
    @Ignore
    public void testUplaodFileNotExist() throws Exception {
        File file = new File("notExist.zip");
        try {
            uploadFile(file);
            Assert.fail();
        } catch (FileNotFoundException e) {

        }

    }

    private File assertUploadedFileExists(final File expectedFile, final String uploadKey)
            throws IOException {
        File restTempDir = new File(CloudifyConstants.REST_FOLDER);
        File uploadsFolder = new File(restTempDir, CloudifyConstants.UPLOADS_FOLDER_NAME);
        Assert.assertNotNull(uploadsFolder);
        File uploadedFileDir = new File(uploadsFolder, uploadKey);
        Assert.assertNotNull(uploadedFileDir);
        Assert.assertTrue(uploadedFileDir.exists());
        Assert.assertTrue(uploadedFileDir.isDirectory());
        Assert.assertTrue(uploadedFileDir.exists());
        Assert.assertTrue(uploadedFileDir.isDirectory());
        File uploadedFile = new File(uploadedFileDir, UPLOADED_FILE_NAME);
        Assert.assertNotNull(uploadedFile);
        Assert.assertTrue(uploadedFile.exists());
        Assert.assertTrue(uploadedFile.isFile());
        Assert.assertTrue(FileUtils.contentEquals(expectedFile, uploadedFile));

        return uploadedFile;
    }

}
