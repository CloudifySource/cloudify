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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.controllers.UploadController;
import org.cloudifysource.rest.repo.UploadRepo;
import org.cloudifysource.restclient.GSRestClient;
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
		"classpath:META-INF/spring/webmvc-config-test.xml" })
public class UploadControllerTest extends ControllerTest {

	private static final String UPLOAD_RESOURCES_PATH = "src" + File.separator + "test" + File.separator 
			+ "resources" + File.separator + "upload";
	private static final String TEST_FILE_PATH =  UPLOAD_RESOURCES_PATH + File.separator + "test.txt";

	private static final String UPLOADED_FILE_NAME = "upload.zip";
	private static final String UPLOAD_URI = "/upload/" + UPLOADED_FILE_NAME;
	
	private HashMap<String, HashMap<RequestMethod, HandlerMethod>> controllerMapping;
	private UploadController controller;
	private UploadRepo uploadRepo;
	
	private static final int UPLOAD_SIZE_LIMIT_BYTES = 10;
	private static final int CLEANUP_TIMOUT_SECONDS = 3;
	

	@Before
	public void init() throws NoSuchMethodException, IOException {
		controller = applicationContext.getBean(UploadController.class);
		uploadRepo = applicationContext.getBean(UploadRepo.class);
		uploadRepo.resetTimeout(CLEANUP_TIMOUT_SECONDS);
		controllerMapping = new HashMap<String, HashMap<RequestMethod, HandlerMethod>>();
		HashMap<RequestMethod, HandlerMethod> map = new HashMap<RequestMethod, HandlerMethod>();
		HandlerMethod method = new HandlerMethod(controller, "upload", String.class, MultipartFile.class);
		map.put(RequestMethod.POST, method);
		controllerMapping.put(UPLOAD_URI, map);
	}
	
	@Override
	public HandlerMethod getExpectedMethod(final String requestUri, final RequestMethod requestMethod) {
		HashMap<RequestMethod, HandlerMethod> hashMap = controllerMapping.get(requestUri);
		Assert.assertNotNull(hashMap);
		return hashMap.get(requestMethod);
	}
	
	private String uploadFile(final File file) throws Exception {
		MockHttpServletResponse response = testPostFile(UPLOAD_URI, file);
		Map<String, Object> responseMap = GSRestClient.jsonToMap(response.getContentAsString());
		String uploadKey = (String) responseMap.get("uploadKey");
		Assert.assertNotNull(uploadKey);
		return uploadKey;
	}
	
	@Test
	public void testUpload() throws Exception {
		File file = new File(TEST_FILE_PATH);
		String uploadKey = uploadFile(file);
		assertUploadedFileExists(file, uploadKey);
	}
	
	@Test
	public void testUploadExceededSizeLimitFile() throws Exception {
		controller.setUploadSizeLimitBytes(UPLOAD_SIZE_LIMIT_BYTES);
		File uploadFile = new File(TEST_FILE_PATH);
		MockHttpServletResponse response = null;
		long fileSize = uploadFile.length();
		try {
			response = testPostFile(UPLOAD_URI, uploadFile);
			Assert.fail("Tring to upload a file of zise " + fileSize + "expected to failed. response " 
			+ response.getContentAsString());
		} catch (RestErrorException e) {
			Map<String, Object> errorDescription = e.getErrorDescription();
			String status = (String) errorDescription.get("status");
			Assert.assertEquals("error", status);
			String errorMsg = (String) errorDescription.get("error");
			Assert.assertEquals(CloudifyMessageKeys.FILE_SIZE_LIMIT_EXCEEDED.getName(), errorMsg);
			Object[] args = (Object[]) errorDescription.get("error_args");
			Object[] expectedArgs = {UPLOADED_FILE_NAME, controller.getUploadSizeLimitBytes(), fileSize};
			Assert.assertArrayEquals(expectedArgs, args);
		}  finally {
			controller.setUploadSizeLimitBytes(CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES);
		}
	}
	
	@Test
	public void testUploadTimeout() throws Exception {
		File file = new File(TEST_FILE_PATH);
		String uploadKey = uploadFile(file);
		File uploadedFile = assertUploadedFileExists(file, uploadKey);
		String parentPath = uploadedFile.getParentFile().getAbsolutePath();
		
		Thread.sleep(CLEANUP_TIMOUT_SECONDS * 2000);
		
		File expectedToBeDeletedFolder = new File(parentPath);
		Assert.assertFalse(expectedToBeDeletedFolder.exists());
	}
	
	private File assertUploadedFileExists(final File expectedFile, final String uploadKey) 
			throws IOException {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File uploadsFolder = new File(tempDir, CloudifyConstants.UPLOADS_FOLDER_NAME);
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
