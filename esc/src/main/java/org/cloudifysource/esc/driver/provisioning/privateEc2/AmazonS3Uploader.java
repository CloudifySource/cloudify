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

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudUser;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.util.TarGzUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
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
 * @since 2.7.0
 * 
 */
public class AmazonS3Uploader {

	private static final String ZIP_PREFIX = "cloudFolder";
	private static final long ONE_DAY_IN_MILLIS = 1000L * 60L * 60L * 24L;

	private final Logger logger = Logger.getLogger(AmazonS3Uploader.class.getName());

	private AmazonS3 s3client;
	private String accessKey;

	public AmazonS3Uploader(final Cloud cloud, final ComputeTemplate managementTemplate) {
		
		final CloudUser user = cloud.getUser();
		accessKey = user.getUser();
		final AWSCredentials credentials = new BasicAWSCredentials(accessKey, user.getApiKey());
		
		String s3LocationId = (String) managementTemplate.getCustom().get("s3LocationId");
		if (StringUtils.isBlank(s3LocationId)) {
			throw new IllegalArgumentException("S3 Location Id not set on the management template");
		}

		final String protocol = (String) cloud.getCustom().get("protocol");
		if (StringUtils.isNotBlank(protocol)) {
			// set the client protocol
			logger.info("setting the S3 client protocol to: " + protocol);
			ClientConfiguration clientConfig = new ClientConfiguration();
			clientConfig.setProtocol(Protocol.valueOf(protocol));
			this.s3client = new AmazonS3Client(credentials, clientConfig);
		} else {
			logger.info("using the default protocol for the S3 client (https)");
			this.s3client = new AmazonS3Client(credentials);	
		}
		
		final String endpoint = (String) cloud.getCustom().get("s3endpoint");
		if (StringUtils.isNotBlank(endpoint)) {
			logger.info("setting S3 endpoint: " + endpoint);
			s3client.setEndpoint(endpoint);
		} else if (StringUtils.isNotBlank(s3LocationId)) {
			Region s3Region = RegionUtils.convertLocationId2Region(s3LocationId);
			logger.info("setting S3 region: " + s3Region);
			this.s3client.setRegion(s3Region);
		} else {
			logger.warning("S3 endpoint and location not set, please set one of them");
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
		final File compressedFile = TarGzUtils.createTarGz(pathFolderToArchive, false);
		final S3Object s3Object = this.uploadFile(existingBucketName, compressedFile);
		final String s3Url = this.generatePresignedURL(s3Object);
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
	public S3Object uploadFile(final String bucketFullPath, final File file) {
		final BucketLifecycleConfiguration.Rule ruleArchiveAndExpire = new BucketLifecycleConfiguration.Rule()
				.withId("Delete cloudFolder archives")
				.withPrefix(this.extractPrefix(bucketFullPath) + ZIP_PREFIX)
				.withExpirationInDays(1)
				.withStatus(BucketLifecycleConfiguration.ENABLED.toString());
		final List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<BucketLifecycleConfiguration.Rule>();
		rules.add(ruleArchiveAndExpire);
		final BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(rules);
		this.s3client.setBucketLifecycleConfiguration(bucketFullPath, configuration);

		final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketFullPath, this.accessKey, file);
		putObjectRequest.setKey(file.getName());
		final ObjectMetadata metadata = new ObjectMetadata();
		putObjectRequest.setMetadata(metadata);
		this.s3client.putObject(putObjectRequest);

		final S3Object object = this.s3client.getObject(bucketFullPath, file.getName());
		return object;
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

	/**
	 * Returns a pre-signed URL for accessing an Amazon S3 resource.
	 * 
	 * @param bucketName
	 *            The bucket where the resource lies.
	 * @param objectKey
	 *            The key object.
	 * @return A pre-signed URL for accessing an Amazon S3 resource.
	 */
	public String generatePresignedURL(final String bucketName, final String objectKey) {
		final Date expiration = new Date();
		expiration.setTime(System.currentTimeMillis() + ONE_DAY_IN_MILLIS);

		final GeneratePresignedUrlRequest generatePresignedUrlRequest =
				new GeneratePresignedUrlRequest(bucketName, objectKey);
		generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
		generatePresignedUrlRequest.setExpiration(expiration);

		URL generatePresignedObjectURL = s3client.generatePresignedUrl(generatePresignedUrlRequest);

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Zip uploaded. Limited signed URL: " + generatePresignedObjectURL);
		}
		return generatePresignedObjectURL.toString();
	}

	/**
	 * Returns a pre-signed URL for accessing an Amazon S3 resource.
	 * 
	 * @param s3
	 *            The S3 object.
	 * @return A pre-signed URL for accessing an Amazon S3 resource.
	 */
	public String generatePresignedURL(final S3Object s3) {
		return this.generatePresignedURL(s3.getBucketName(), s3.getKey());
	}

	/**
	 * Delete uploaded files from S3.
	 * 
	 * @param bucketName
	 *            The name of the bucket.
	 * @param key
	 *            The resource's key.
	 * */
	public void deleteS3Object(final String bucketName, final String key) {
		try {
			logger.fine("Delete S3 resource: bucketName=" + bucketName + ", key=" + key);
			s3client.deleteObject(bucketName, key);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "Couldn't delete files from S3 : bucketName=" + bucketName + ", keys=" + key);
		}
	}

}
