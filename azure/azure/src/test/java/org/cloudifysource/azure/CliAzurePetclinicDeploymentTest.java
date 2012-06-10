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
import org.cloudifysource.azure.test.utils.RepetativeConditionProvider;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

public class CliAzurePetclinicDeploymentTest extends AbstractCliAzureDeploymentTest {

    // path to petclinic application relative to cloudify installation
    private static final String RELATIVE_APPLICATION_EXAMPLE_PATH = "recipes\\apps\\petclinic-simple-azure";

    // arguments for cli
    private static final int TIMEOUT_IN_MINUTES = 60;
    private static final int POLLING_INTERVAL_IN_MINUTES = 1;
    private static final String AZURE_HOSTED_SERVICE = "petclinicl100";
    private static final String APPLICATION_NAME = "petclinic";
    private static final AzureSlot AZURE_SLOT = AzureSlot.Staging;
    private static final String RDP_PFX_FILE_PASSWORD = "123456";
    private static final String INITIAL_NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE = "1";
    private static final String NUMBER_OF_INSTANCES_FOR_TOMCAT_SERVICE = "2";
    private static final String TOMCAT_SERVICE = "tomcat";

    //petclinic
    // expected number of instances on azure after successful bootstrap
    // 2 mgt machines
    // 1 webui machine
    // 1 tomcat machine
    // 1 mongos machine
    // 1 mongoConfig machine
    // 2 mongod machines

    //petclinic-simple
    // expected number of instances on azure after successful bootstrap
    // 2 mgt machines
    // 1 webui machine
    // 1 tomcat machine
    // 1 mongod machine
    private static final int EXPECTED_NUMBER_OF_MACHINES = 5; //petclinic = 8

    @Test(timeout = 120 * 60 * 1000L)
    public void repeatTest() throws Throwable {
        DateFormat df = new SimpleDateFormat("_yyyy-MM-dd_hh-mm");
        int repeat = 1;
        for (int i = 1; i <= repeat; i++) {
            //overwrites any existing file with that name.
            String filePattern = "azuretest" + i + df.format(new Date()) + ".log";
            FileHandler fileHandler = new FileHandler(filePattern);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            logger.info("Starting test iteration #" + i);
            boolean failed = false;
            try {
                before(RELATIVE_APPLICATION_EXAMPLE_PATH, AZURE_HOSTED_SERVICE, AZURE_SLOT);
                test();
            } catch (Throwable t) {
                failed = true;
                logger.log(Level.SEVERE,"Test failed with the following exception",t);
                throw t;
            } finally {
                if (failed) {
                    logger.info("Failed test iteration #" + i + ". Machines are left running for manual diagnostics");
                    logger.removeHandler(fileHandler);
                    try {
                        SimpleMail.send("Azure "+APPLICATION_NAME+" test failed\nSubscription ID=" + credentials.getHostedServicesSubscriptionId(), new File(filePattern));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to send email", e);
                    }
                    after(AZURE_HOSTED_SERVICE, TIMEOUT_IN_MINUTES, POLLING_INTERVAL_IN_MINUTES);
                    
                    // no need to break since an exception was raised and is going to fail the test
                } else {
                    logger.info("Passed test iteration #" + i);
                    logger.removeHandler(fileHandler);
                    try {
                        SimpleMail.send("Azure test passed\nSubscription ID=" + credentials.getHostedServicesSubscriptionId(), new File(filePattern));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to send email", e);
                    }
                    after(AZURE_HOSTED_SERVICE, TIMEOUT_IN_MINUTES, POLLING_INTERVAL_IN_MINUTES);
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
                "-azure-svc", AZURE_HOSTED_SERVICE,
                "-azure-pwd", RDP_PFX_FILE_PASSWORD,
                "-azure-location", "'" + AZURE_REGION + "'",
                applicationAbsolutePath
        );

        commands.add(boostrapApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();

        String deploymentUrl = deployment.getUrl();

        final URL restAdminMachinesUrl = getMachinesUrl(deploymentUrl);

        log("Getting number of running machines");

        repetativeAssert("Number of machines", new RepetativeConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
                    logger.info("Actual numberOfMachines=" + numberOfMachines + ". Expected numberOfMachins=" + EXPECTED_NUMBER_OF_MACHINES);
                    return EXPECTED_NUMBER_OF_MACHINES == numberOfMachines;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while calculating numberOfMachines", e);
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

        final URI applicationUrl = getApplicationUrl(deploymentUrl,"petclinic-mongo").toURI();

        RepetativeConditionProvider applicationInstalledCondition = new RepetativeConditionProvider() {
            @Override
            public boolean getCondition() {
                try {
                    URL url = applicationUrl.toURL();
                    return isUrlAvailable(url);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while checking if " + applicationUrl.toString() + " is available", e);
                    return false;
                }
            }
        };

        repetativeAssert("Failed waiting for application: " + applicationUrl, applicationInstalledCondition);

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
            @Override
            public boolean getCondition() {
                try {
                    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
                    logger.info("Actual numberOfMachines=" + numberOfMachines + ". Expected numberOfMachins=" + (EXPECTED_NUMBER_OF_MACHINES + 1));
                    return numberOfMachines == EXPECTED_NUMBER_OF_MACHINES + 1;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while calculating numberOfMachines", e);
                    return false;
                }
            }
        });

        List<String> uninstallApplicationCommand = Arrays.asList(
                "uninstall-application",
                "--verbose",
                "-timeout", String.valueOf(TIMEOUT_IN_MINUTES),
                APPLICATION_NAME
        );

        commands.add(connectCommand);
        commands.add(uninstallApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();

        Assert.assertFalse("Petclinic application should not be running", isUrlAvailable(applicationUrl.toURL()));

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
            @Override
            public boolean getCondition() {
                try {
                    int numberOfMachines = getNumberOfMachines(restAdminMachinesUrl);
                    logger.info("Actual numberOfMachines=" + numberOfMachines + ". Expected numberOfMachins=" + EXPECTED_NUMBER_OF_MACHINES);
                    return EXPECTED_NUMBER_OF_MACHINES == numberOfMachines;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception while calculating numberOfMachines", e);
                    return false;
                }
            }
        });

        commands.add(connectCommand);
        commands.add(installApplicationCommand);
        runCliCommands(cliExecutablePath, commands, isDebugMode);
        commands.clear();

        repetativeAssert("Failed waiting for petclinic application", applicationInstalledCondition);

    }


}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               