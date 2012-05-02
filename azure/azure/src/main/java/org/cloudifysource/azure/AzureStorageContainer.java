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
package org.cloudifysource.azure;

import java.io.IOException;
import java.net.URI;

import org.soyatec.windowsazure.blob.BlobStorageClient;
import org.soyatec.windowsazure.blob.IBlob;
import org.soyatec.windowsazure.blob.IBlobContainer;
import org.soyatec.windowsazure.blob.IBlockBlob;
import org.soyatec.windowsazure.blob.internal.ContainerAccessControl;
import org.soyatec.windowsazure.internal.util.NameValueCollection;

public class AzureStorageContainer {

    private final String accountName;
    private final String accountKey;
    private final String containerName;
    
    
    private IBlobContainer container;
    
    public AzureStorageContainer(String accountName, String accountKey, String containerName) {
        this.accountName = accountName;
        this.accountKey = accountKey;
        this.containerName = containerName;
    }

    public void connect() {
    	BlobStorageClient client = AzureStorageUtils.createStorageAccess(accountName, accountKey);
        boolean containerExist = client.isContainerExist(containerName);
        if (!containerExist) {
        	boolean isPublicContainer = true;
        	//creates a container with public access control settings
        	client.createContainer(
        			containerName, 
        			new NameValueCollection(), 
        			new ContainerAccessControl(isPublicContainer));
        }
        container = client.getBlobContainer(containerName);
    }
    
    public URI putBlob(String blobName, String sourceFilePath) throws IOException {
    	IBlockBlob blockBlob ;
    	
    	if (isBlobExists(blobName)) {
    		blockBlob = AzureStorageUtils.updateBlockBlob(container,blobName,sourceFilePath);
			
        }
    	else {
    		blockBlob =AzureStorageUtils.createBlockBlob(container, blobName, sourceFilePath);
    	}
    	
    	return getUri(blockBlob);
    }

    public void deleteBlob(String blobName) throws IOException {
        container.deleteBlob(blobName);
    }
    
    public boolean isBlobExists(String blobName) {
        return container.isBlobExist(blobName);
    }

    public URI getBlobUri(String blobName) {
        return getUri(container.getBlobReference(blobName));
    }
    
    private URI getUri(IBlob blob) {
    	return blob.getProperties().getUri();
    }
    
}
