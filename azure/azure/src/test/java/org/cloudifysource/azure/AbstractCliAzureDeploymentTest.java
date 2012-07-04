/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.cloudifysource.azure.files.AzureDeploymentConfigurationFile;
import org.cloudifysource.azure.test.utils.RepetativeConditionProvider;
import org.cloudifysource.azure.test.utils.TestUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractCliAzureDeploymentTest {
    protected static final Logger logger = Logger.getLogger(CliAzureTravelDeploymentTest.class.getName());

    protected static void log(String message) {
        logger.log(Level.INFO, message);
    }

    protected static void log(String message, Throwable t) {
        logger.log(Level.INFO, message, t);
    }

    protected static final int POLLING_INTERVAL_IN_MILLIS = 5000;
    protected static final int TIMEOUT_IN_MILLIS = 60 * 60 * 1000;

    protected static final String AZURE_REGION = "North Central US";
    //private static final String AZURE_REGION="South Central US";

    // keys and partial values for azure.properties
    protected static final String AZURE_PROPERITES_SUBSCRIPTION_ID_KEY = "subscriptionId";
    protected static final String AZURE_PROPERITES_CERTIFICATE_THUMBPRINT_KEY = "certificateThumbprint";
    protected static final String AZURE_PROPERTIES_ACCOUNT_NAME_KEY = "storageAccount";
    protected static final String AZURE_PROPERTIES_ACCOUNT_KEY_KEY = "storageAccessKey";
    protected static final String AZURE_PROPERTIES_CONTAINER_NAME_KEY = "storageBlobContainerName";
    protected static final String AZURE_PROPERTIES_CS_PACK_FOLDER_KEY = "cspackFolder";
    protected static final String AZURE_PROPERTIES_WORKER_ROLE_FOLDER_KEY = "workerRoleFolder";
    protected static final String AZURE_PROPERTIES_RDP_CERT_FILE_KEY = "rdpCertFile";
    protected static final String AZURE_PROPERTIES_RDP_PFX_FILE_KEY = "rdpPfxFile";
    protected static final String AZURE_PROPERTIES_RDP_LOGIN_USERNAME_KEY = "rdpLoginUsername";
    protected static final String AZURE_PROPERTIES_RDP_LOGIN_ENCRYPTED_PASSWORD = "rdpLoginEncrypedPassword";


    protected static final String AZURE_CONTAINER_NAME = "packages-public";
    protected static final String CS_PACK_FOLDER = "C:\\Program Files\\Windows Azure SDK\\v1.6\\bin";
    protected static final String RELATIVE_WORKER_ROLE_DIR = "plugins\\azure\\WorkerRoles\\GigaSpacesWorkerRoles";
    protected static final String RDP_CERT_FILE = "plugins\\azure\\azure_rdp.cer";
    protected static final String RDP_PFX_FILE = "plugins\\azure\\azure_rdp.pfx";
    protected static final String AZURE_RDP_LOGIN_USERNAME = "gigaspaces";
    protected static final String AZURE_RDP_LOGIN_ENCRYPTED_PASSWORD = "MIIBnQYJKoZIhvcNAQcDoIIBjjCCAYoCAQAxggFOM" +
            "IIBSgIBADAyMB4xHDAaBgNVBAMME1dpbmRvd3MgQXp1cmUgVG9vbHMCEHajSi1yL1OxQL/rs764sEwwDQYJKoZIhvcNAQEBBQA" +
            "EggEAh8XuwaqYEwajHLWBtB2xYnJJuQ1qS7l54hu9XOkIqpZ0pRBeVLUJf3O6jowmxPU0/6m65JazTOo0MWVQce48kpEO7VBs2" +
            "CW26g1PJjVyxyDD+z1e+3kdmNoMMjPKvt3vaHq5ZCol+iO8yWMXDITc2l6EPJay4QBMeGd3CPGgn04inD5P08YwaicEqNZk+Sj" +
            "MZXVNyFExNWEPDvhFKw6qRPbu1i1nwp6yMjHFImB7yjrK8zgkWdKMyxNuThtTLLWzwESN0yaSjSp4BWCUNTmNyM9UC88UTQk3U" +
            "GnEmNRY6KMmyBt+rO8KNZvtFqWDV+ygEfFaj17ft5PpsoOk2Ue/sTAzBgkqhkiG9w0BBwEwFAYIKoZIhvcNAwcECGmQUBVuWTl" +
            "WgBDXc+tBT2eo8ktROXDG7VDc";



    // path to cloudify installation relative to localWorkingdir
    protected static final String GIGASPACES_LATEST_HOME = "gigaspaces";

    // paths relative to cloudify installation folder
    protected static final String RELATIVE_CLI_PATH = "tools/cli/cloudify.bat";
    protected static final String RELATIVE_AZURE_PROPERTIES_PATH = "tools/cli/plugins/azure/azure.properties";
    protected static final String RELATIVE_AZURE_CONFIG_EXEC_PATH = "tools/cli/plugins/azure/azureconfig.exe";

    // system properties
    protected static final String IS_DEBUG_MODE_SYSTEM_PROPERTY = "test.debug.mode";
    protected static final String LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY = "local.working.dir";

    protected static final AzureCredentials credentials = new AzureCredentials();

    protected File cliExecutablePath;
    protected AzureDeploymentWrapper deployment;
    protected File applicationFile;
    protected boolean isDebugMode;

    //@Before
    public void before(String relativeApplicationExamplePath, String azureHostedService, AzureSlot azureSlot) throws Exception {

        isDebugMode = Boolean.parseBoolean(System.getProperty(IS_DEBUG_MODE_SYSTEM_PROPERTY, Boolean.toString(false)));

        String localWorkingDirPath = System.getProperty(LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY);
        Assert.assertNotNull(LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY + " system property has to exists", localWorkingDirPath);

        File localWorkingDir = new File(localWorkingDirPath);
        Assert.assertTrue(localWorkingDir.getPath() + " does not exist", localWorkingDir.isDirectory());

        File gigaSpacesCloudifyDir = new File(localWorkingDir, GIGASPACES_LATEST_HOME);
        Assert.assertTrue(gigaSpacesCloudifyDir.getPath() + " does not exist", gigaSpacesCloudifyDir.isDirectory());

        applicationFile = new File(gigaSpacesCloudifyDir, relativeApplicationExamplePath);
        Assert.assertTrue(applicationFile.getPath() + " does not exist", applicationFile.isDirectory());

        // these should exist assuming the cloudify folder is valid
        cliExecutablePath = new File(gigaSpacesCloudifyDir, RELATIVE_CLI_PATH);
        File azureConfigExec = new File(gigaSpacesCloudifyDir, RELATIVE_AZURE_CONFIG_EXEC_PATH);
        File azurePropertiesFile = new File(gigaSpacesCloudifyDir, RELATIVE_AZURE_PROPERTIES_PATH);
        File cscfgFile = new File(cliExecutablePath.getParent(), RELATIVE_WORKER_ROLE_DIR + "\\ServiceConfiguration.Cloud.cscfg");

        // update worker roles configuration to upload logs
        AzureDeploymentConfigurationFile cscfg = new AzureDeploymentConfigurationFile(cscfgFile);
        cscfg.setUploadAgentLogs(true);
        cscfg.setUploadAllLogs(true);
        cscfg.flush();

        Properties newAzureProps = new Properties();
        newAzureProps.setProperty(AZURE_PROPERTIES_ACCOUNT_NAME_KEY, credentials.getBlobStorageAccountName());
        newAzureProps.setProperty(AZURE_PROPERTIES_ACCOUNT_KEY_KEY, credentials.getBlobStorageAccountKey());
        newAzureProps.setProperty(AZURE_PROPERTIES_CONTAINER_NAME_KEY, AZURE_CONTAINER_NAME);
        newAzureProps.setProperty(AZURE_PROPERTIES_WORKER_ROLE_FOLDER_KEY, RELATIVE_WORKER_ROLE_DIR);
        newAzureProps.setProperty(AZURE_PROPERTIES_CS_PACK_FOLDER_KEY, CS_PACK_FOLDER);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_CERT_FILE_KEY, RDP_CERT_FILE);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_PFX_FILE_KEY, RDP_PFX_FILE);
        newAzureProps.setProperty(AZURE_PROPERITES_CERTIFICATE_THUMBPRINT_KEY, credentials.getHostedServicesCertificateThumbrint());
        newAzureProps.setProperty(AZURE_PROPERITES_SUBSCRIPTION_ID_KEY, credentials.getHostedServicesSubscriptionId());
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_LOGIN_USERNAME_KEY, AZURE_RDP_LOGIN_USERNAME);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_LOGIN_ENCRYPTED_PASSWORD, AZURE_RDP_LOGIN_ENCRYPTED_PASSWORD);

        log("Overriding azure.properties file");
        FileOutputStream fos = new FileOutputStream(azurePropertiesFile);
        newAzureProps.store(fos, null);
        fos.close();

        deployment = new AzureDeploymentWrapper(azureConfigExec,
                credentials.getHostedServicesSubscriptionId(), credentials.getHostedServicesCertificateThumbrint(),
                azureHostedService, azureSlot,
                null, null, null, null);

    }

    //    @After
    public void after(String azureHostedService, int timeoutInMinutes, int pollingIntervalInMinutes) throws IOException, InterruptedException {
        if (cliExecutablePath != null) {
            log("Destroying deployment");

            List<List<String>> commands = new ArrayList<List<String>>();
            List<String> azureTeardownApplication = Arrays.asList(
                    "azure:teardown-app",
                    "--verbose",
                    "-azure-svc", azureHostedService,
                    "-timeout", String.valueOf(timeoutInMinutes),
                    "-progress", String.valueOf(pollingIntervalInMinutes)
            );

            commands.add(azureTeardownApplication);

            runCliCommands(cliExecutablePath, commands, isDebugMode);
            commands.clear();
        }
    }

    /**
     * This methods extracts the number of machines running gs-agents using the rest admin api
     *
     * @param machinesRestAdminUrl
     * @return number of machines running gs-agents
     * @throws IOException
     * @throws java.net.URISyntaxException
     */
    protected static int getNumberOfMachines(URL machinesRestAdminUrl) throws IOException, URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(machinesRestAdminUrl.toURI());
        try {
            String json = client.execute(httpGet, new BasicResponseHandler());
            Matcher matcher = Pattern.compile("\"Size\":\"([0-9]+)\"").matcher(json);
            if (matcher.find()) {
                String rawSize = matcher.group(1);
                int size = Integer.parseInt(rawSize);
                return size;
            } else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    protected static String stripSlash(String str) {
        if (str == null || !str.endsWith("/")) {
            return str;
        }
        return str.substring(0, str.length() - 1);
    }

    protected static URL getApplicationUrl(String url, String warName) throws Exception {
        return new URL(stripSlash(url) + "/"+warName+"/");
    }

    protected static URL getMachinesUrl(String url) throws Exception {
        return new URL(stripSlash(url) + "/rest/admin/machines");
    }

    public static String runCliCommands(File cliExecutablePath, List<List<String>> commands, boolean isDebug) throws IOException, InterruptedException {
        if (!cliExecutablePath.isFile()) {
            throw new IllegalArgumentException(cliExecutablePath + " is not a file");
        }

        File workingDirectory = cliExecutablePath.getAbsoluteFile().getParentFile();
        if (!workingDirectory.isDirectory()) {
            throw new IllegalArgumentException(workingDirectory + " is not a directory");
        }

        int argsCount = 0;
        for (List<String> command : commands) {
            argsCount += command.size();
        }

        // needed to properly intercept error return code
        String[] cmd = new String[(argsCount == 0 ? 0 : 1) + 4 /* cmd /c call cloudify.bat ["args"] */];
        int i = 0;
        cmd[i] = "cmd";
        i++;
        cmd[i] = "/c";
        i++;
        cmd[i] = "call";
        i++;
        cmd[i] = cliExecutablePath.getAbsolutePath();
        i++;
        if (argsCount > 0) {
            cmd[i] = "\"";
            //TODO: Use StringBuilder
            for (List<String> command : commands) {
                if (command.size() > 0) {
                    for (String arg : command) {
                        if (cmd[i].length() > 0) {
                            cmd[i] += " ";
                        }
                        cmd[i] += arg;
                    }
                    cmd[i] += ";";
                }
            }
            cmd[i] += "\"";
        }
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDirectory);
        pb.redirectErrorStream(true);

        String extCloudifyJavaOptions = "";

        if (isDebug) {
            extCloudifyJavaOptions += "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9000 -Xnoagent -Djava.compiler=NONE";
        }

        pb.environment().put("EXT_CLOUDIFY_JAVA_OPTIONS", extCloudifyJavaOptions);
        final StringBuilder sb = new StringBuilder();

        logger.info("running: " + cliExecutablePath + " " + Arrays.toString(cmd));

        // log std output and redirected std error
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            sb.append(line).append('\n');
            line = reader.readLine();
            logger.info(line);
        }

        final String readResult = sb.toString();
        final int exitValue = p.waitFor();

        logger.info("Exit value = " + exitValue);
        if (exitValue != 0) {
            Assert.fail("Cli ended with error code: " + exitValue);
        }
        return readResult;
    }

    protected static void repetativeAssert(String message, RepetativeConditionProvider condition) throws InterruptedException {
        TestUtils.repetativeAssertTrue(message, condition, POLLING_INTERVAL_IN_MILLIS, TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    protected static boolean isUrlAvailable(URL url) throws URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url.toURI());
        httpGet.addHeader("Cache-Control", "no-cache");
        try {
            HttpResponse response = client.execute(httpGet);
            logger.info("HTTP GET " + url + " returned " + response.getStatusLine().getReasonPhrase() +" Response: ["+response.getStatusLine().getStatusCode()+"] "+EntityUtils.toString(response.getEntity()));
            if (response.getStatusLine().getStatusCode() == 404 || response.getStatusLine().getStatusCode() == 502) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log("Failed connecting to " + url, e);
            return false;
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
