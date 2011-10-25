package org.openspaces.cloud.azure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.openspaces.cloud.azure.files.AzureDeploymentConfigurationFile;
import org.openspaces.cloud.azure.test.utils.RepetativeConditionProvider;
import org.openspaces.cloud.azure.test.utils.TestUtils;

public class AzureDeloymentTest {

   private static final Logger logger = Logger.getLogger(AzureDeloymentTest.class.getName());
    
   private static final int POLLING_INTERVAL_IN_MILLIS = 5000;
   private static final int TIMEOUT_IN_MILLIS = 60 * 60 * 1000;
   
   private static final String HOSTED_SERVICE = "springdemo";
   private static final AzureSlot AZURE_SLOT = AzureSlot.Staging;
   private static final String DEPLOYMENT_NAME = "travel";
   private static final String DEPLOYMENT_LABEL = "travel";
   private static final String PACKAGE_BLOB_NAME = "travelnew.cspkg";
   private static final String GIGASPACES_XAP_BLOB_NAME = "gigaspaces-latest.zip";
   private static final String AZURE_ACCOUNT_NAME = "gigaspaces";
   private static final String AZURE_ACCOUNT_KEY = "6RjDHthW5QRP9aycTl5DX1wKuCpHWx/NwlqDUQs8iIdHwLobWfBZRcWvaLoHH8ac7qSJ5K1jnEWC7SrU6FF8+Q==";
   private static final String AZURE_CONTAINER_NAME = "packages-public";
    
   private static final int EXPECTED_NUMBER_OF_MACHINES = 7;
   
   private AzureDeploymentWrapper deployment;
   private AzureStorageContainer container;
   
   @Before
   public void before() throws Exception {
       
       String localWorkingDir = System.getProperty("local.working.dir");
       Assert.assertNotNull("local.working.dir must be passed", localWorkingDir);

       File azureConfigExe = new File(localWorkingDir, "azureconfig.exe");
       Assert.assertTrue("azureconfig.exe must exist", azureConfigExe.isFile());
       
       File cscfgFile = new File(localWorkingDir, "travel.cscfg");
       Assert.assertTrue(cscfgFile.getAbsolutePath() + " does not exist", cscfgFile.isFile());
       
       String localPackagePath = new File(localWorkingDir, "travel.cspkg").getAbsolutePath();      
       Assert.assertTrue(localPackagePath + " must exist", new File(localPackagePath).isFile());
       
       container = new AzureStorageContainer(AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY, AZURE_CONTAINER_NAME);
       
       log("Connecting to Azure Blob Store");
       container.connect();

       
       log("Uploading new package to store");
       container.putBlob(PACKAGE_BLOB_NAME, localPackagePath);

       String gigaSpacesXAPLocalFilePath = System.getProperty("gigaspaces.xap.filepath");
       if (gigaSpacesXAPLocalFilePath != null && new File(gigaSpacesXAPLocalFilePath).isFile()) {
           
           if (container.isBlobExists(GIGASPACES_XAP_BLOB_NAME)) {
               log("Deleting previous gigaspaces-latest blob");
               container.deleteBlob(GIGASPACES_XAP_BLOB_NAME);
           }
           
           log("Uploading gigaspaces xap from " + gigaSpacesXAPLocalFilePath + " to blob store");
           container.putBlob(GIGASPACES_XAP_BLOB_NAME, gigaSpacesXAPLocalFilePath);
           
           log("Updating gigaspaces xap url in configuration file");
           AzureDeploymentConfigurationFile configFile = new AzureDeploymentConfigurationFile(cscfgFile);
           configFile.setGigaSpacesXAPDownloadUrl(container.getBlobUri(GIGASPACES_XAP_BLOB_NAME));
           configFile.flush();
       }
       
       deployment = new AzureDeploymentWrapper(azureConfigExe, null, null,
               HOSTED_SERVICE, AZURE_SLOT, DEPLOYMENT_NAME, DEPLOYMENT_LABEL, cscfgFile, 
               container.getBlobUri(PACKAGE_BLOB_NAME));
       
       deployment.setStatusTimeoutInMillis(TIMEOUT_IN_MILLIS);
       deployment.setStatusPollingIntervalInMillis(POLLING_INTERVAL_IN_MILLIS);
       
       Assert.assertTrue("Application is already deployed", deployment.getStatus() == AzureDeploymentStatus.NotFound);
       
       log("Deploying package to azure");
       deployment.deploy();
       Assert.assertTrue(deployment.getStatus() == AzureDeploymentStatus.Running);
   }
   
//    @Test(timeout = 120 * 60 * 1000)
    public void test() throws IOException, InterruptedException {

        log("Getting deployment url");
        final URL machinesRestAdminUrl = new URL(getMachinesUrl(deployment.getUrl()));
        
        log("Waiting for WebUI to finish loading");
        TestUtils.repetativeAssertTrue("Failed while waiting for WebUI to start", 
            new RepetativeConditionProvider() {
                public boolean getCondition() {
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection)machinesRestAdminUrl.openConnection();
                        connection.getInputStream();
                        connection.disconnect();
                        return true;
                    } catch (IOException e) {
                        log("Not connected yet.. retrying");
                        return false;
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
        }, POLLING_INTERVAL_IN_MILLIS, TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
        
        log("Waiting for expected number of machines");
        TestUtils.repetativeAssertTrue("Failed while waiting for number of instances", 
            new RepetativeConditionProvider() {
                public boolean getCondition() {
                    try {
                        int machinesSize = getNumberOfMachines(machinesRestAdminUrl);
                        boolean condition = machinesSize == EXPECTED_NUMBER_OF_MACHINES;
                        if (!condition) {
                            log("Number of machines is currently: " + machinesSize + ", waiting for it to become " + EXPECTED_NUMBER_OF_MACHINES);
                        }
                        return condition;
                    } catch (IOException e) {
                        log("Caugt exeption: " + e.getMessage() + " while waiting for number of instances. retrying...");
                        return false;
                    }
                }
        } , POLLING_INTERVAL_IN_MILLIS, TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
        
    }
    
    @After
    public void after() {
        if (deployment != null && deployment.getStatus() != AzureDeploymentStatus.NotFound) {
            log("Stopping deployment");
            deployment.stop();
            Assert.assertTrue(deployment.getStatus() == AzureDeploymentStatus.Suspended);
            
            log("Removing deployment");
            deployment.delete();
            Assert.assertTrue(deployment.getStatus() == AzureDeploymentStatus.NotFound);        
        }
    }
    
    /**
     * This methods extracts the number of machines running gs-agents using the rest admin api 
     *  
     * @param machinesRestAdminUrl
     * @return number of machines running gs-agents
     * @throws IOException
     */
    private static int getNumberOfMachines(URL machinesRestAdminUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)machinesRestAdminUrl.openConnection();
        try {
        InputStream is = connection.getInputStream();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        if (line != null) {
            sb.append(line);
        }
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();
        Pattern pattern = Pattern.compile("\"Size\":\"([0-9]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String rawSize = matcher.group(1);
            int size = Integer.parseInt(rawSize);
            return size;
        } else {
            return 0;
        }
        } finally {
           connection.disconnect();
        }
    }
    
    private static String getRestAdminUrl(String url) {
    	if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        return url + ":8100/";
    }
    
    private static String getMachinesUrl(String url) {
        return getRestAdminUrl(url) + "admin/machines";
    }

    private static void log(String message) {
        logger.log(Level.INFO, message);
    }
    
}
