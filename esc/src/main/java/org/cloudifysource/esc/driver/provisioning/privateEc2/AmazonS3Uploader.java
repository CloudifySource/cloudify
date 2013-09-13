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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Class to help uploading file to Amazon S3.
 * 
 * @author victor
 * 
 */
public class AmazonS3Uploader {

	private static final String ZIP_PREFIX = "cloudFolder";
	private static final long ONE_DAY_IN_MILLIS = 1000L * 60L * 60L * 24L;

	private final Logger logger = Logger.getLogger(AmazonS3Uploader.class.getName());

	private AmazonS3 s3client;
	private String accessKey;

	public AmazonS3Uploader(final String accessKey, final String secretKey) {
		this(accessKey, secretKey, null);
	}

	public AmazonS3Uploader(final String accessKey, final String secretKey, final String locationId) {
		this.accessKey = accessKey;
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		this.s3client = new AmazonS3Client(credentials);
		if (locationId != null) {
			this.s3client.setRegion(RegionUtils.convertLocationId2Region(locationId));
		}
	}

	/**
	 * Compress and upload a folder.
	 * 
	 * @param existingBucketName
	 *            The name of the bucket where to download the file.
	 * @param pathFolderToArchive
	 *            The folder to upload.
	 * @return The URL to access the file in s3
	 * @exception IOException
	 *                When the compression fails.
	 */
	public String compressAndUploadToS3(final String existingBucketName, final String pathFolderToArchive)
			throws IOException {
		// File compressedFile = this.zipFolder(pathFolderToArchive);
		// File compressedFile = TarGzUtils.addFolderToTarGz(pathFolderToArchive);
		String s3Url = this.uploadFile(existingBucketName, null);
		return s3Url;
	}

	/**
	 * Upload file.
	 * 
	 * @param bucketFullPath
	 *            The path of the bucket where to download the file.
	 * @param file
	 *            The file to upload.
	 * @return The URL to access the file in s3
	 */
	public String uploadFile(final String bucketFullPath, final File file) {
		BucketLifecycleConfiguration.Rule ruleArchiveAndExpire = new BucketLifecycleConfiguration.Rule()
				.withId("Delete cloudFolder archives")
				.withPrefix(this.extractPrefix(bucketFullPath) + ZIP_PREFIX)
				.withExpirationInDays(1)
				.withStatus(BucketLifecycleConfiguration.ENABLED.toString());
		List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<BucketLifecycleConfiguration.Rule>();
		rules.add(ruleArchiveAndExpire);
		BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(rules);
		this.s3client.setBucketLifecycleConfiguration(bucketFullPath, configuration);

		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketFullPath, this.accessKey, file);
		putObjectRequest.setKey(file.getName());
		ObjectMetadata metadata = new ObjectMetadata();
		putObjectRequest.setMetadata(metadata);
		this.s3client.putObject(putObjectRequest);

		S3Object object = this.s3client.getObject(bucketFullPath, file.getName());

		URL generatePresignedObjectURL = this.generatePresignedObjectURL(object.getBucketName(), object.getKey());
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Zip uploaded. Limited signed URL: " + generatePresignedObjectURL);
		}
		return generatePresignedObjectURL.toString();
	}

	private String extractPrefix(final String bucketFullPath) {
		String prefix = null;
		if (bucketFullPath.contains("/")) {
			prefix = bucketFullPath.
					substring(bucketFullPath.indexOf("/") + 1, bucketFullPath.length()) + "/";
		} else {
			prefix = "/";
		}
		return prefix;
	}

	private URL generatePresignedObjectURL(final String bucketName, final String objectKey) {
		Date expiration = new Date();
		expiration.setTime(System.currentTimeMillis() + ONE_DAY_IN_MILLIS);

		GeneratePresignedUrlRequest generatePresignedUrlRequest =
				new GeneratePresignedUrlRequest(bucketName, objectKey);
		generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
		generatePresignedUrlRequest.setExpiration(expiration);

		return s3client.generatePresignedUrl(generatePresignedUrlRequest);
	}

}
