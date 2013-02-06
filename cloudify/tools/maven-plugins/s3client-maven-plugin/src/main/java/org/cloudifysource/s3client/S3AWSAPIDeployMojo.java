/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
 *******************************************************************************/
package org.cloudifysource.s3client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jclouds.blobstore.BlobStoreContext;

import java.io.File;

/**
 * Deploy to S3 storage
 * 
 * @goal deploy-native
 * @aggregator true
 */
public class S3AWSAPIDeployMojo extends AbstractMojo {

	/**
	 * The S3 user
	 * 
	 * @parameter
	 *  expression="${put.user}"
	 *  default-value=""
	 */
	private String user;

	/**
	 * The S3 key
	 * 
	 * @parameter
	 *  expression="${put.key}"
	 *  default-value=""
	 */
	private String key;

    /**
     * The file to put
     *
     * @parameter
     *  expression="${put.source}"
     *  type="java.io.File"
     *  default-value=""
     */
    private File source;

    /**
     * The target path of the blob
     *
     * @parameter
     *  expression="${put.target}"
     *  default-value=""
     */
    private String target;

	/**
	 * The containter to put into
	 * 
	 * @parameter 
	 *  expression="${put.container}" 
	 *  default-value=""
	 */
	private String container;


	/**
	 * Should the blob have a public url
	 * 
	 * @parameter 
	 *  expression="${put.publicUrl}" 
	 *  default-value=""
	 */
	private String publicUrl;
	
	
	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
	    BlobStoreContext context = null;

		try {
            getLog().info("Using aws-sdk-java");
            AWSCredentials awsCredentials = new BasicAWSCredentials(user, key);
            AmazonS3 s3 = new AmazonS3Client(awsCredentials);
			
			uploadFile(s3, source, target);

		} catch (Exception e) {
			throw new MojoFailureException("Failed put operation", e);
		} finally {
			if (context != null) {
				context.close();
			}
		}

	}

	private void uploadFile(AmazonS3 s3, File source, String target) throws MojoFailureException{
		if (source.isDirectory()){
			for (File f : source.listFiles()){
				uploadFile(s3, new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName());
			}
		}
		else{
			getLog().info("Processing " + source  + ", upload size is: " + (source).length() + ". Target: " + target);
            s3.putObject(new PutObjectRequest(target, key, source).withCannedAcl(CannedAccessControlList.PublicRead));
			getLog().info("Upload of " + source + " was ended successfully");

		}

	}
	
	public String getLocalRepo(){
	    String localRepoProp = System.getProperty("maven.repo.local");
        if(localRepoProp != null)
	        return localRepoProp;
        return System.getProperty("user.home") + "/.m2/repository";
	}
}