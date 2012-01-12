package org.cloudifysource.s3client;

import java.io.File;
import java.util.HashSet;
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
            Blob newBlob = store.blobBuilder(target)
                .payload(source)
                .build();

            getLog().info("upload size is: " + source.length());
            getLog().info("waiting for upload to end");
            
            store.putBlob(container, newBlob);
            
            if (publicUrl != null && Boolean.parseBoolean(publicUrl)) {
                S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
                String ownerId = client.getObjectACL(container, target).getOwner().getId();
                
                client.putObjectACL(container, target, 
                        AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
            }
                
        } catch (Exception e) {
            throw new MojoFailureException("Failed put operation", e);
        } finally {
            if (context != null) {
                context.close();
            }
        }
        
    }
    
}
