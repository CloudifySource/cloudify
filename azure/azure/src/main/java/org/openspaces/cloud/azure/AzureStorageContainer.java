package org.openspaces.cloud.azure;

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
    
    private BlobStorageClient client;
    private IBlobContainer container;
    
    public AzureStorageContainer(String accountName, String accountKey, String containerName) {
        this.accountName = accountName;
        this.accountKey = accountKey;
        this.containerName = containerName;
    }

    public void connect() {
        client = AzureStorageUtils.createStorageAccess(accountName, accountKey);
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
