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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.AccessControlList;
import org.jclouds.s3.domain.CannedAccessPolicy;

import com.google.inject.Module;

/**
 * Put a blob in S3 storage
 * 
 * @goal put
 * @aggregator true
 */
public class S3PutMojo extends AbstractMojo {

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
	 * The containter to put into
	 * 
	 * @parameter 
	 *  expression="${put.container}" 
	 *  default-value=""
	 */
	private String container;

	/**
	 * The target path of the blob
	 * 
	 * @parameter 
	 *  expression="${put.target}" 
	 *  default-value=""
	 */
	private String target;

	/**
	 * Should the blob have a public url
	 * 
	 * @parameter 
	 *  expression="${put.publicUrl}" 
	 *  default-value=""
	 */
	private String publicUrl;

	public void execute() throws MojoExecutionException, MojoFailureException {

		BlobStoreContext context = null;

		try {
			Set<Module> wiring = new HashSet<Module>();
			context = new BlobStoreContextFactory().createContext("aws-s3", user, key, wiring, new Properties());

			BlobStore store = context.getBlobStore();

			uploadFile( source, target, store);
			
			/*if (source.isDirectory())
			{
				getLog().info("Source " + source + " is a directory. Adding all files");

				for (File f : source. listFiles())
				{
					getLog().info("Processing " + source + "/" + f.getName() + ", upload size is: " + (source+"/"+f.getName()).length() + ". Target: " + target + "/" + f.getName());
					store.putBlob(container, store.blobBuilder(target + "/" + f.getName())
							.payload(new File(source.getPath() + "/" + f.getName()))
							.build());
					getLog().info("Upload of " + source + "/" + f.getName() + " was ended successfully");

			 if (publicUrl != null && Boolean.parseBoolean(publicUrl)) {
						S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
						String ownerId = client.getObjectACL(container, target+"/"+f.getName()).getOwner().getId();

						client. putObjectACL(container, target+"/"+f.getName(), 
								AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
					}
				}

			}
			else
			{
				Blob newBlob = store.blobBuilder(target)
				.payload(source)
				.build();
				getLog().info("upload size is: " + source.length());
				getLog().info("waiting for upload to end");
				store.putBlob(container, newBlob);
				
				if (publicUrl != null && Boolean.parseBoolean(publicUrl)) {
					S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
					String ownerId = client.getObjectACL(container, target).getOwner().getId();

					client. putObjectACL(container, target, 
							AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
				}

			}*/


		} catch (Exception e) {
			throw new MojoFailureException("Failed put operation", e);
		} finally {
			if (context != null) {
				context.close();
			}
		}

	}
	
	private void uploadFile(File source, String target, BlobStore store) throws MojoFailureException
	{
		if (source.isDirectory())
		{
			for (File f : source.listFiles())
			{
				uploadFile(new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName(), store);
			}
		}
		else
		{
			getLog().info("Processing " + source  + ", upload size is: " + (source).length() + ". Target: " + target);
	
			store.putBlob(container, store.blobBuilder(target)
					.payload(source)
					.build());
			getLog().info("Upload of " + source + " was ended successfully");
		}
			
	}

}
