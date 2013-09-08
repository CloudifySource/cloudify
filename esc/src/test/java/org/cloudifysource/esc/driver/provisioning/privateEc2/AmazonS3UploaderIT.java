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
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class AmazonS3UploaderIT {

	private AmazonS3Uploader s3Uploader;

	@Before
	public void before() throws IOException {
		Properties awscredentials = new Properties();
		awscredentials.load(new FileInputStream("./cloudify/clouds/privateEc2/privateEc2-cloud.properties"));
		String accessKey = ((String) awscredentials.get("accessKey")).replaceAll("\"", "");
		String secretKey = ((String) awscredentials.get("apiKey")).replaceAll("\"", "");
		this.s3Uploader = new AmazonS3Uploader(accessKey, secretKey, "eu-west-1");
	}

	@Test
	public void testUploadFile() throws Exception {
		String uploadFile = this.s3Uploader.uploadFile("cloudify-eu/test2", new File("./privateEc2.zip"));
		System.out.println(uploadFile);
	}

	@Test
	public void testZipFolder() throws Exception {
		File zipFile = this.s3Uploader
				.zipFolder("C:/cloudify-deployment/gigaspaces-cloudify-2.6.1-ga-b5199-139/clouds/privateEc2");
		System.out.println(zipFile);
	}

	@Test
	public void testZipAndUploadToS3() throws Exception {
		String s3File = this.s3Uploader.zipAndUploadToS3(
				"cloudify-eu/test",
				"C:/cloudify-deployment/gigaspaces-cloudify-2.6.1-ga-b5199-139/clouds/privateEc2");
		System.out.println(s3File);
	}

}
