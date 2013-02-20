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

import com.google.inject.Module;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.AccessControlList;
import org.jclouds.s3.domain.CannedAccessPolicy;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Deploy to S3 storage
 * 
 * @goal deploy
 * @aggregator true
 */
public class S3DeployMojo extends AbstractMojo {

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
			Set<Module> wiring = new HashSet<Module>();
			context = new BlobStoreContextFactory().createContext("aws-s3", user, key, wiring, new Properties());
			S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());

			BlobStore store = context.getBlobStore();

			String path = project.getGroupId().replace(".", "/") + "/" + project.getArtifactId() + "/" + project.getVersion();
			File source = new File(getLocalRepo() + "/" + path);
			
			uploadFile(source, path, store, client);

		} catch (Exception e) {
			throw new MojoFailureException("Failed put operation", e);
		} finally {
			if (context != null) {
				context.close();
			}
		}

	}

	private void uploadFile(File source, String target, BlobStore store, S3Client client) throws MojoFailureException
	{
		if (source.isDirectory())
		{
			for (File f : source.listFiles())
			{
				uploadFile(new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName(), store, client);
			}
		}
		else
		{
			getLog().info("Processing " + source  + ", upload size is: " + (source).length() + ". Target: " + target);
			store.putBlob(container, store.blobBuilder(target)
					.payload(source)
					.build());
			getLog().info("Upload of " + source + " was ended successfully");

			if (publicUrl != null && Boolean.parseBoolean(publicUrl)) {
				String ownerId = client.getObjectACL(container, target).getOwner().getId();
				client.putObjectACL(container, target, 
						AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
			}
		}

	}
	
	public String getLocalRepo(){
	    String localRepoProp = System.getProperty("maven.repo.local");
        if(localRepoProp != null)
	        return localRepoProp;
        return System.getProperty("user.home") + "/.m2/repository";
	}
}