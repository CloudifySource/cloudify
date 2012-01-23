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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.azure.files.AzureDeploymentConfigurationFile;
import org.cloudifysource.azure.test.utils.RepetativeConditionProvider;
import org.cloudifysource.azure.test.utils.TestUtils;
import org.junit.Test;

public class CliAzureDeploymentTest {
    
    private static final Logger logger = Logger.getLogger(CliAzureDeploymentTest.class.getName());
    private static void log(String message) { logger.log(Level.INFO, message); }
    private static void log(String message, Throwable t) { logger.log(Level.INFO, message, t); }
    
    private static final int POLLING_INTERVAL_IN_MILLIS = 5000;
    private static final int TIMEOUT_IN_MILLIS = 60 * 60 * 1000;
    
    // keys and partial values for azure.properties
    private static final String AZURE_PROPERITES_SUBSCRIPTION_ID_KEY = "subscriptionId";
    private static final String AZURE_PROPERITES_CERTIFICATE_THUMBPRINT_KEY = "certificateThumbprint";
    private static final String AZURE_PROPERTIES_ACCOUNT_NAME_KEY = "storageAccount";
    private static final String AZURE_PROPERTIES_ACCOUNT_KEY_KEY = "storageAccessKey";
    private static final String AZURE_PROPERTIES_CONTAINER_NAME_KEY = "storageBlobContainerName";
    private static final String AZURE_PROPERTIES_CS_PACK_FOLDER_KEY = "cspackFolder";
    private static final String AZURE_PROPERTIES_WORKER_ROLE_FOLDER_KEY = "workerRoleFolder";
    private static final String AZURE_PROPERTIES_RDP_CERT_FILE_KEY = "rdpCertFile";
    private static final String AZURE_PROPERTIES_RDP_PFX_FILE_KEY = "rdpPfxFile";
    private static final String AZURE_PROPERTIES_RDP_LOGIN_USERNAME_KEY = "rdpLoginUsername";
    private static final String AZURE_PROPERTIES_RDP_LOGIN_ENCRYPTED_PASSWORD = "rdpLoginEncrypedPassword";
    
	// azure account
	// -------------
    //private static final String AZURE_SUBSCRIPTION_ID = "9f24fac0-f989-4873-b3d5-6886fbc6cd29";
    //private static final String AZURE_ACCOUNT_NAME = "gigaspaces";
    //private static final String AZURE_ACCOUNT_KEY = "UD+sH0G99u9Rjt/jD5U39k2exfUQAgmeIgXx6+s/LTwLwTeScC7YK+53+UXkh9cZI4yiv2ZpKrnXYh9/e5eNhw==";
	//private static final String AZURE_REGION="South Central US";
	
	// azure partner account
	// ---------------------
	private static final String AZURE_SUBSCRIPTION_ID = "2719917d-5e33-4aaa-9fee-429290752498"; 
    private static final String AZURE_ACCOUNT_NAME = "gigaspaces3";
    private static final String AZURE_ACCOUNT_KEY = "2hQ0Kljm3tWj49kUrHfFypnd8KyOT1nlsi766M6dHJYgpHjEy+CfR2922cfFzTvqCN94SSkcx7GG+8KovxV2mQ==";
	private static final String AZURE_REGION="North Central US";
	
    private static final String AZURE_CERTIFICATE_THUMBPRINT = "9E0086E300D5B2F7CC00E734F58FFB1661920FE9";
    private static final String AZURE_CONTAINER_NAME = "packages-public";
    private static final String CS_PACK_FOLDER = "C:\\Program Files\\Windows Azure SDK\\v1.4\\bin";
    private static final String RELATIVE_WORKER_ROLE_DIR = "plugins\\azure\\WorkerRoles\\GigaSpacesWorkerRoles";
    private static final String RDP_CERT_FILE = "plugins\\azure\\azure_rdp.cer";
    private static final String RDP_PFX_FILE = "plugins\\azure\\azure_rdp.pfx";
    private static final String AZURE_RDP_LOGIN_USERNAME = "gigaspaces"; 
    private static final String AZURE_RDP_LOGIN_ENCRYPTED_PASSWORD = "MIIBnQYJKoZIhvcNAQcDoIIBjjCCAYoCAQAxggFOM" +
            "IIBSgIBADAyMB4xHDAaBgNVBAMME1dpbmRvd3MgQXp1cmUgVG9vbHMCEHajSi1yL1OxQL/rs764sEwwDQYJKoZIhvcNAQEBBQA" +
            "EggEAh8XuwaqYEwajHLWBtB2xYnJJuQ1qS7l54hu9XOkIqpZ0pRBeVLUJf3O6jowmxPU0/6m65JazTOo0MWVQce48kpEO7VBs2" +
            "CW26g1PJjVyxyDD+z1e+3kdmNoMMjPKvt3vaHq5ZCol+iO8yWMXDITc2l6EPJay4QBMeGd3CPGgn04inD5P08YwaicEqNZk+Sj" +
            "MZXVNyFExNWEPDvhFKw6qRPbu1i1nwp6yMjHFImB7yjrK8zgkWdKMyxNuThtTLLWzwESN0yaSjSp4BWCUNTmNyM9UC88UTQk3U" +
            "GnEmNRY6KMmyBt+rO8KNZvtFqWDV+ygEfFaj17ft5PpsoOk2Ue/sTAzBgkqhkiG9w0BBwEwFAYIKoZIhvcNAwcECGmQUBVuWTl" +
            "WgBDXc+tBT2eo8ktROXDG7VDc";

    // path to travel application relative to cloudify installation
	private static final String RELATIVE_APPLICATION_EXAMPLE_PATH = "examples\\azure\\travel";

    // arguments for cli
    private static final int TIMEOUT_IN_MINUTES = 60;
    private static final int POLLING_INTERVAL_IN_MINUTES = 1;
    private static final String AZURE_HOSTED_SERVICE = "travel100";
    private static final String APPLICATION_NAME = "travel";
    private static final AzureSlot AZURE_SLOT = AzureSlot.Staging;
    private static final String RDP_PFX_FILE_PASSWORD = "123456";
    private static final String INITIAL_NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE = "1";
    private static final String NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE = "2";
    private static final String TOMCAT_SERVICE = "tomcat";
    
    // path to cloudify installation relative to localWorkingdir
    private static final String GIGASPACES_LATEST_HOME = "gigaspaces";
    
    // paths relative to cloudify installation folder
    private static final String RELATIVE_CLI_PATH = "tools/cli/cloudify.bat";
    private static final String RELATIVE_AZURE_PROPERTIES_PATH = "tools/cli/plugins/azure/azure.properties";
    private static final String RELATIVE_AZURE_CONFIG_EXEC_PATH = "tools/cli/plugins/azure/azureconfig.exe";
    
    // expected number of instances on azure after successful bootstrap
    private static final int EXPECTED_NUMBER_OF_MACHINES = 5;
    
    // system properties 
    private static final String IS_DEBUG_MODE_SYSTEM_PROPERTY = "test.debug.mode";
    private static final String LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY = "local.working.dir";
    
    private File cliExecutablePath;
    private AzureDeploymentWrapper deployment;
    private File applicationFile;
    private boolean isDebugMode;
    
    //@Before
    public void before() throws Exception {
        
        isDebugMode = Boolean.valueOf(System.getProperty(IS_DEBUG_MODE_SYSTEM_PROPERTY, Boolean.toString(false)));
        
        String localWorkingDirPath = System.getProperty(LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY);
        Assert.assertNotNull(LOCAL_WORKING_DIR_SYSTEM_PROPERTY_KEY + " system property has to exists", localWorkingDirPath);
        
        File localWorkingDir = new File(localWorkingDirPath);
        Assert.assertTrue(localWorkingDir.getPath() + " does not exist", localWorkingDir.isDirectory());
        
        File gigaSpacesCloudifyDir = new File(localWorkingDir, GIGASPACES_LATEST_HOME);
        Assert.assertTrue(gigaSpacesCloudifyDir.getPath() + " does not exist", gigaSpacesCloudifyDir.isDirectory());
        
		applicationFile = new File(gigaSpacesCloudifyDir, RELATIVE_APPLICATION_EXAMPLE_PATH);
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
        newAzureProps.setProperty(AZURE_PROPERTIES_ACCOUNT_NAME_KEY, AZURE_ACCOUNT_NAME);
        newAzureProps.setProperty(AZURE_PROPERTIES_ACCOUNT_KEY_KEY, AZURE_ACCOUNT_KEY);
        newAzureProps.setProperty(AZURE_PROPERTIES_CONTAINER_NAME_KEY, AZURE_CONTAINER_NAME);
        newAzureProps.setProperty(AZURE_PROPERTIES_WORKER_ROLE_FOLDER_KEY, RELATIVE_WORKER_ROLE_DIR);
        newAzureProps.setProperty(AZURE_PROPERTIES_CS_PACK_FOLDER_KEY, CS_PACK_FOLDER);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_CERT_FILE_KEY, RDP_CERT_FILE);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_PFX_FILE_KEY, RDP_PFX_FILE);
        newAzureProps.setProperty(AZURE_PROPERITES_CERTIFICATE_THUMBPRINT_KEY, AZURE_CERTIFICATE_THUMBPRINT);
        newAzureProps.setProperty(AZURE_PROPERITES_SUBSCRIPTION_ID_KEY, AZURE_SUBSCRIPTION_ID);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_LOGIN_USERNAME_KEY, AZURE_RDP_LOGIN_USERNAME);
        newAzureProps.setProperty(AZURE_PROPERTIES_RDP_LOGIN_ENCRYPTED_PASSWORD, AZURE_RDP_LOGIN_ENCRYPTED_PASSWORD);
        
        log("Overriding azure.properties file");
        FileOutputStream fos = new FileOutputStream(azurePropertiesFile);
        newAzureProps.store(fos, null);      
        fos.close();
        
        deployment = new AzureDeploymentWrapper(azureConfigExec, 
            AZURE_SUBSCRIPTION_ID, AZURE_CERTIFICATE_THUMBPRINT, 
            AZURE_HOSTED_SERVICE, AZURE_SLOT, 
            null, null, null, null);
        
    }
    
	@Test(timeout = 120 * 60 * 1000L)
    public void repeatTest() throws Throwable {
		DateFormat df = new SimpleDateFormat("_yyyy-MM-dd_hh-mm");
		int repeat = 1;
		for (int i=1 ; i<=repeat; i++) {
		    //overwrites any existing file with that name.
			String filePattern = "azuretest"+i+df.format(new Date())+".log";
			FileHandler fileHandler = new FileHandler(filePattern);
			fileHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(fileHandler);
	
			logger.info("Starting test iteration #"+i);
			boolean failed = false;
			try {
				before();
				test();
			}
			catch (Throwable t) {
				failed = true;
				throw t;
			}
			finally {
				if (failed) {
					logger.info("Failed test iteration #"+i +". Machines are left running for manual diagnostics");
					logger.removeHandler(fileHandler);
					try {
						SimpleMail.send("Azure test failed\nSubscription ID="+AZURE_SUBSCRIPTION_ID, new File(filePattern));
					} catch(Exception e) {
						logger.log(Level.SEVERE,"Failed to send email",e);
					}
					after();
					// no need to break since an exception was raised and is going to fail the test
				}
				else {
					logger.info("Passed test iteration #"+i);
					logger.removeHandler(fileHandler);
					try {
						SimpleMail.send("Azure test passed\nSubscription ID="+AZURE_SUBSCRIPTION_ID, new File(filePattern));
					} catch(Exception e) {
						logger.log(Level.SEVERE,"Failed to send email",e);
					}
					after();
					// no need to break since we want to test the run multiple times (or until it fails)
				}
			}
		}
	}
	
    //@Test(timeout = 120 * 60 * 1000)
    public void test() throws Exception {
        List<List<String>> commands = new ArrayList<List<String>>();

    	// CLI uses groovy to parse the path which requires either a double backslash or a single slash
    	String applicationAbsolutePath = applicationFile.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/");        
    	
    	List<String> boostrapApplicationCommand = Arrays.asList(
			"azure:bootstrap-app",
			"--verbose",
			"-timeout", String.valueOf(TIMEOUT_IN_MINUTES),
			"-progress", String.valueOf(POLLING_INTERVAL_IN_MINUTES),
			"-azure-svc", AZURE_HOSTED_SERVICE ,
			"-azure-pwd", RDP_PFX_FILE_PASSWORD,
			"-azure-location","'"+AZURE_REGION+"'",
			applicationAbsolutePath
    	);
        
        commands.add(boostrapApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
        
        String deploymentUrl = deployment.getUrl();
        
        final URL restAdminMachinesUrl = getMachinesUrl(deploymentUrl);
        
        log("Getting number of running machines");
        
        repetativeAssert("Number of machines", new RepetativeConditionProvider() {
            public boolean getCondition() {
                try {
				    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
					logger.info("Actual numberOfMachines=" + numberOfMachines +". Expected numberOfMachins="+EXPECTED_NUMBER_OF_MACHINES);
                    return EXPECTED_NUMBER_OF_MACHINES == numberOfMachines;
                } catch (Exception e) {
				    logger.log(Level.WARNING, "Exception while calculating numberOfMachines",e);
                    return false;
                }
            }
        });

        List<String> connectCommand = Arrays.asList(
             "azure:connect-app",
             "--verbose",
			 "-timeout 5",
             "-azure-svc", AZURE_HOSTED_SERVICE
        );
        
        List<String> installApplicationCommand = Arrays.asList(
            "install-application", 
            "--verbose",
            applicationAbsolutePath
        );
        
        commands.add(connectCommand);
        commands.add(installApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
        
        final URI travelApplicationUrl = getTravelApplicationUrl(deploymentUrl).toURI();

        RepetativeConditionProvider applicationInstalledCondition = new RepetativeConditionProvider() {
            public boolean getCondition() {
				try {
					URL url = travelApplicationUrl.toURL();
				    return isUrlAvailable(url);
                } catch (Exception e) {
					logger.log(Level.WARNING, "Exception while checking if "+ travelApplicationUrl.toString() + " is available",e);
                    return false;
                }              
            }
        };
        
        repetativeAssert("Failed waiting for travel application", applicationInstalledCondition);

        List<String> setInstancesScaleOutCommand = Arrays.asList(
             "azure:set-instances",
             "--verbose",
             "-azure-svc", AZURE_HOSTED_SERVICE,
             TOMCAT_SERVICE, NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE
        );
        
        commands.add(connectCommand);
        commands.add(setInstancesScaleOutCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
        
        repetativeAssert("Failed waiting for scale out", new RepetativeConditionProvider() {
            public boolean getCondition() {
                try {
				    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
					logger.info("Actual numberOfMachines=" + numberOfMachines +". Expected numberOfMachins="+(EXPECTED_NUMBER_OF_MACHINES+1));
                    return numberOfMachines == EXPECTED_NUMBER_OF_MACHINES + 1;
                } catch (Exception e) {
					logger.log(Level.WARNING, "Exception while calculating numberOfMachines",e);
                    return false;
                }
            }
        });
      
        List<String> uninstallApplicationCommand = Arrays.asList(
            "uninstall-application", 
            "--verbose",
            "-timeout", String.valueOf(TIMEOUT_IN_MINUTES),
            "-progress", String.valueOf(POLLING_INTERVAL_IN_MINUTES),
			APPLICATION_NAME
        );

        commands.add(connectCommand);
        commands.add(uninstallApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
    	
        Assert.assertFalse("Travel application should not be running", isUrlAvailable(travelApplicationUrl.toURL()));
        
        List<String> setInstancesScaleInCommand = Arrays.asList(
            "azure:set-instances",
            "--verbose",
            "-azure-svc", AZURE_HOSTED_SERVICE,
            TOMCAT_SERVICE, INITIAL_NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE
        );
        
        commands.add(connectCommand);
        commands.add(setInstancesScaleInCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
        
        repetativeAssert("Failed waiting for scale in", new RepetativeConditionProvider() {
            public boolean getCondition() {
                try {
				    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
					logger.info("Actual numberOfMachines=" + numberOfMachines +". Expected numberOfMachins="+EXPECTED_NUMBER_OF_MACHINES);
                    return EXPECTED_NUMBER_OF_MACHINES == numberOfMachines;
                } catch (Exception e) {
				    logger.log(Level.WARNING, "Exception while calculating numberOfMachines",e);
                    return false;
                }
            }
        });
       
        commands.add(connectCommand);
        commands.add(installApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();
        
        repetativeAssert("Failed waiting for travel application", applicationInstalledCondition);
        
    }

//    @After
    public void after() throws IOException, InterruptedException {
    	if (cliExecutablePath != null) {
	    	log("Destroying deployment");
	        
	        List<List<String>> commands = new ArrayList<List<String>>();
	        List<String> azureTeardownApplication = Arrays.asList(
	                "azure:teardown-app",
	                "--verbose",
	                "-azure-svc", AZURE_HOSTED_SERVICE,
	                "-timeout", String.valueOf(TIMEOUT_IN_MINUTES),
	                "-progress", String.valueOf(POLLING_INTERVAL_IN_MINUTES)
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
     * @throws URISyntaxException 
     */
    private static int getNumberOfMachines(URL machinesRestAdminUrl) throws IOException, URISyntaxException {
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
    
    private static String stripSlash(String str) {
        if (str == null || !str.endsWith("/")) {
            return str;
        }
        return str.substring(0, str.length()-1);
    }
    
    private static URL getTravelApplicationUrl(String url) throws Exception {
        return new URL(stripSlash(url) + "/travel/");        
    }
    
    private static URL getMachinesUrl(String url) throws Exception {
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
        String[] cmd = new String[(argsCount==0?0:1) + 4 /* cmd /c call cloudify.bat ["args"] */];
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
						if (cmd[i].length() >0) {
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
            sb.append(line).append("\n");
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
    
    private static void repetativeAssert(String message, RepetativeConditionProvider condition) throws InterruptedException {
        TestUtils.repetativeAssertTrue(message, condition, POLLING_INTERVAL_IN_MILLIS, TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
    }
    
    private static boolean isUrlAvailable(URL url) throws URISyntaxException {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url.toURI());
		httpGet.addHeader("Cache-Control", "no-cache");
        try {
            HttpResponse response = client.execute(httpGet);
			System.out.print("HTTP GET " + url + "Response:");
			response.getEntity().writeTo(System.out);
			System.out.print("");
			if (response.getStatusLine().getStatusCode() == 404) {
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
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               