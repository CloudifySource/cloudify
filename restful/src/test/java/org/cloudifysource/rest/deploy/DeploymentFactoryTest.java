/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import junit.framework.Assert;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openspaces.admin.internal.pu.elastic.config.AbstractElasticProcessingUnitConfig;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ElasticStatefulProcessingUnitConfig;
import org.openspaces.admin.pu.elastic.config.ElasticStatelessProcessingUnitConfig;
import org.openspaces.admin.pu.elastic.config.ScaleStrategyConfig;
import org.openspaces.admin.space.ElasticSpaceConfig;
import org.openspaces.admin.space.ElasticSpaceDeployment;

public class DeploymentFactoryTest {

    private static final String USM_SERVICE_FILE = "src/test/resources/deploy/usm-service.groovy";
    private static final String STATEFUL_SERVICE_FILE = "src/test/resources/deploy/stateful-service.groovy";
    private static final String STATEFUL_PACKED_FILE = "src/test/resources/deploy/packedStatefulPU.zip";
    private static final String STATELESS_PACKED_FILE = "src/test/resources/deploy/packedStatelessPU.zip";
    private static final String STATELESS_SERVICE_FILE = "src/test/resources/deploy/stateless-service.groovy";
    private static final String DATAGRID_SERVICE_FILE = "src/test/resources/deploy/DataGrid-service.groovy";
    private static final String CLOUD_FILE = "src/test/resources/deploy/ec2-cloud.groovy";
    //
    private static ElasticProcessingUnitDeploymentFactoryImpl deploymentFactory;
    private static Cloud cloud;

    @BeforeClass
    public static void beforeClass()
            throws Exception {
        deploymentFactory = new ElasticProcessingUnitDeploymentFactoryImpl(null);
        cloud = ServiceReader.readCloud(new File(CLOUD_FILE));
    }

    @Test
    public void testDependsOnWithService() throws Exception {

        final Service service = ServiceReader.readService(new File(USM_SERVICE_FILE));
        DeploymentConfig deploymentConfig = createDeploymentConfig(false, service);
        ElasticStatelessProcessingUnitDeployment deployment =
                (ElasticStatelessProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        ElasticStatelessProcessingUnitConfig deploymentHolder = deployment.create();
        String dependsOn = deploymentHolder.getContextProperties().get(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
        Assert.assertEquals("[]", dependsOn);

    }

    @Test
    public void testElasticUSMDeploymentIntegrity() throws Exception {

        final Service service = ServiceReader.readService(new File(USM_SERVICE_FILE));
        //cloud deployment assertions
        DeploymentConfig deploymentConfig = createDeploymentConfig(false, service);

        ElasticStatelessProcessingUnitDeployment deployment =
                (ElasticStatelessProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        ElasticStatelessProcessingUnitConfig deploymentHolder = deployment.create();

        assertSharedCloudDeploymentPropertiesSet(deploymentHolder);
        assertStatelessUSMDeploymentIntegrity(deploymentHolder);

        //localcloud deployment assertions
        deploymentConfig = createDeploymentConfig(true, service);
        deployment = (ElasticStatelessProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        deploymentHolder = deployment.create();
        assertSharedLocalcloudProperties(deploymentHolder);
        assertStatelessUSMLocalcloudProperties(deploymentHolder);
    }


    private void assertStatelessUSMLocalcloudProperties(
            final ElasticStatelessProcessingUnitConfig deploymentHolder) {
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("memory capacity should be set to 512",
                deploymentHolder.getScaleStrategy().getProperties().get("memory-capacity-megabytes").equals("512"));
        Assert.assertTrue("container memory capacity should be set to 512",
                elasticProperties.get("container.memory-capacity").equals("512"));
        Assert.assertEquals(CloudifyConstants.LOCAL_CLOUD_NAME, deploymentHolder.getContextProperties().get(CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME));
    }

    private void assertSharedLocalcloudProperties(
            final AbstractElasticProcessingUnitConfig deploymentHolder) {


        Assert.assertTrue("shared isolation should be null",
                deploymentHolder.getSharedIsolation() == null);

        //assert context properties
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("deployment ID context property not set",
                contextProperties.get("com.gs.cloudify.deployment-id").equals("deploymentID"));
        Assert.assertTrue("authgroups context property not set",
                contextProperties.get("com.gs.deployment.auth.groups").equals("DUMMY_AUTHGROUPS"));
        Assert.assertTrue("elastic context property not set",
                contextProperties.get("com.gs.service.elastic").equals("true"));
        Assert.assertTrue("application name context property not set",
                contextProperties.get("com.gs.application").equals("default"));
        Assert.assertTrue("debug mode context property not set",
                contextProperties.get("com.gs.service.debug.mode").equals("instead"));
        Assert.assertTrue("debug all context property not set",
                contextProperties.get("com.gs.service.debug.all").equals("true"));

        //assert elastic properties
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("elastic isolation property should be set to true",
                elasticProperties.get("elastic-machine-isolation-public-id").equals("true"));

        //assert machine provisioning
        ElasticMachineProvisioningConfig machineProvisioning = deploymentHolder.getMachineProvisioning();
        Assert.assertTrue("reserved capacity for machine should be 1024MB",
                machineProvisioning.getReservedCapacityPerMachine().toString().equals("256MB RAM"));
        Assert.assertTrue("min number of CPU cores should be 1",
                machineProvisioning.getMinimumNumberOfCpuCoresPerMachine() == 0.0);
        Assert.assertTrue("reserved memory for management should not be set",
                machineProvisioning.getReservedCapacityPerManagementMachine().toString().equals("256MB RAM"));
        Assert.assertTrue("reserved memory for management should not be set",
                machineProvisioning.getReservedCapacityPerMachine().toString().equals("256MB RAM"));

        //assert machine provisioning props.
        final Map<String, String> machineProps = machineProvisioning.getProperties();
        Assert.assertTrue("reserved memory per management machine should not be set",
                machineProps.get("reserved-memory-capacity-per-management-machine-megabytes").equals("256"));
        Assert.assertTrue("reserved memory per machine should be set to 1024",
                machineProps.get("reserved-memory-capacity-per-machine-megabytes").equals("256"));
    }

    private void assertStatelessUSMDeploymentIntegrity(final ElasticStatelessProcessingUnitConfig deploymentHolder) {

        Assert.assertTrue("public isolation should be set to true",
                deploymentHolder.getPublicIsolationConfig());

        //assert context properties
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("asyn install context property not set",
                contextProperties.get("com.gs.cloudify.async-install").equals("true"));
        Assert.assertTrue("service type context property not set",
                contextProperties.get("com.gs.service.type").equals("WEB_SERVER"));

        //assert elastic properties
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("public isolation should be set to true",
                elasticProperties.get("elastic-machine-isolation-public-id").equals("true"));
        Assert.assertTrue("container memory capacity should be set to 128MB",
                elasticProperties.get("container.memory-capacity").equals("128"));

        //assert scale strategy
        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("scale stategy memory capacity in MB is expected to be 128",
                scaleStrategy.getProperties().get("memory-capacity-megabytes").equals("128"));

        //assert machine provisioning props.
        final ElasticMachineProvisioningConfig machineProvisioning = deploymentHolder.getMachineProvisioning();
        final Map<String, String> machineProps = machineProvisioning.getProperties();
        Assert.assertTrue("dedicated-management-machines should be set to true",
                machineProps.get("dedicated-management-machines").equals("true"));
        
        Assert.assertEquals(this.cloud.getName(), deploymentHolder.getContextProperties().get(CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME));
    }

    private DeploymentConfig createDeploymentConfig(final boolean isLocalcloud, final Service service) {
        DeploymentConfig deploymentConfig = new DeploymentConfig();
        final InstallServiceRequest installRequest = new InstallServiceRequest();
        installRequest.setSelfHealing(true);
        installRequest.setDebugAll(true);
        deploymentConfig.setInstallRequest(installRequest);
        if (isLocalcloud) {
            deploymentConfig.setCloud(null);
        } else {
            deploymentConfig.setCloud(cloud);
        }
        deploymentConfig.setService(service);
        deploymentConfig.setAbsolutePUName("default.simple");
        deploymentConfig.setApplicationName("default");
        deploymentConfig.setCloudConfig(new byte[1]);
        deploymentConfig.setDeploymentId("deploymentID");
        deploymentConfig.setLocators("locators");
        deploymentConfig.setPackedFile(new File("DUMMY_FILE"));
        deploymentConfig.setTemplateName("SMALL_LINUX");
        deploymentConfig.setCloudOverrides("envVariable=DEFAULT_OVERRIDES_ENV_VARIABLE");
        deploymentConfig.setAuthGroups("DUMMY_AUTHGROUPS");
        return deploymentConfig;
    }

    
    @Test
    public void testElasticStatfulDeploymentIntegrity() throws Exception {
        final Service service = ServiceReader.readService(new File(STATEFUL_SERVICE_FILE));
        //cloud deployment assertions
        DeploymentConfig deploymentConfig = createDeploymentConfig(false, service);
        //override dummy file.
        deploymentConfig.setPackedFile(new File(STATEFUL_PACKED_FILE));
        ElasticStatefulProcessingUnitDeployment deployment =
                (ElasticStatefulProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        ElasticStatefulProcessingUnitConfig deploymentHolder = deployment.create();
        assertSharedCloudDeploymentPropertiesSet(deploymentHolder);
        assertStatefulDeploymentIntegrity(deploymentHolder);

        //localcloud deployment assertions
        deploymentConfig = createDeploymentConfig(true, service);
      //override dummy file.
        deploymentConfig.setPackedFile(new File(STATEFUL_PACKED_FILE));
        //creating deployment
        deployment = (ElasticStatefulProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        deploymentHolder = deployment.create();
        assertSharedLocalcloudProperties(deploymentHolder);
        assertStatefulLocalcloudProperties(deploymentHolder);
    }

    private void assertStatefulLocalcloudProperties(
            final ElasticStatefulProcessingUnitConfig deploymentHolder) {
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("container memory capacity should be set to 128",
                elasticProperties.get("container.memory-capacity").equals("128"));
        Assert.assertTrue("schema should be set to partitioned-sync2backup",
                elasticProperties.get("schema").equals("partitioned-sync2backup"));
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("mirror-interval context property is ont set",
                contextProperties.get("cluster-config.mirror-service.interval-opers").equals("1000"));
        //assert scale strategy
        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("memory capacity is expected to be ",
                scaleStrategy.getProperties().get("memory-capacity-megabytes").equals("256"));
    }

    private void assertSharedCloudDeploymentPropertiesSet(final AbstractElasticProcessingUnitConfig deploymentHolder) {
        String commandlineArgs = Arrays.toString(deploymentHolder.getCommandLineArguments());
        Assert.assertTrue("container LRMI bind port property missing",
                commandlineArgs.contains("-Dcom.gs.transport_protocol.lrmi.bind-port=7010-7110"));
        Assert.assertTrue("container max memory property missing",
                commandlineArgs.contains("-Xmx128m"));
        Assert.assertTrue("container min memory property missing",
                commandlineArgs.contains("-Xms128m"));

        Assert.assertTrue("shared isolation should be null",
                deploymentHolder.getSharedIsolation() == null);

        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("deployment ID context property not set",
                contextProperties.get("com.gs.cloudify.deployment-id").equals("deploymentID"));
        Assert.assertTrue("authgroups context property not set",
                contextProperties.get("com.gs.deployment.auth.groups").equals("DUMMY_AUTHGROUPS"));
        Assert.assertTrue("elastic context property not set",
                contextProperties.get("com.gs.service.elastic").equals("true"));
        Assert.assertTrue("application name context property not set",
                contextProperties.get("com.gs.application").equals("default"));
        Assert.assertTrue("debug mode context property not set",
                contextProperties.get("com.gs.service.debug.mode").equals("instead"));
        Assert.assertTrue("debug all context property not set",
                contextProperties.get("com.gs.service.debug.all").equals("true"));

        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("container command line argument is missing  bind protocol or memory settings",
                elasticProperties.get("container.commandline-arguments").
                        equals("-Dcom.gs.transport_protocol.lrmi.bind-port=7010-7110 -Xmx128m -Xms128m"));

        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("cpu capacity cores is expected to be equal to 0",
                scaleStrategy.getProperties().get("cpu-capacity-cores").equals("0.0"));
        //zones property is no longer used.
        Assert.assertTrue("zones usage should be set to false",
                scaleStrategy.getProperties().get("enable-agent-zones-aware").equals("false"));

        final ElasticMachineProvisioningConfig machineProvisioning = deploymentHolder.getMachineProvisioning();
        Assert.assertTrue("reserved capacity for machine should be 1024MB",
                machineProvisioning.getReservedCapacityPerMachine().toString().equals("1024MB RAM"));
        Assert.assertTrue("min number of CPU cores should be 1",
                machineProvisioning.getMinimumNumberOfCpuCoresPerMachine() == 1.0);
        Assert.assertTrue("reserved memory for management should not be set",
                machineProvisioning.getReservedCapacityPerManagementMachine().toString().equals("0MB RAM"));

        final Map<String, String> machineProps = machineProvisioning.getProperties();
        Assert.assertTrue("locators value is missing",
                machineProps.get("locator").equals("locators"));
        Assert.assertTrue("config directory dows not match template definition",
                machineProps.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION_DIRECTORY)
                        .equals("/home/ec2-user/gs-files"));
        Assert.assertTrue("template name missing from machine provisioning props",
                machineProps.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME).equals("SMALL_LINUX"));
        Assert.assertTrue("reserved memory per management machine should not be set",
                machineProps.get("reserved-memory-capacity-per-management-machine-megabytes").equals("0"));
        Assert.assertTrue("reserved memory per machine should be set to 1024",
                machineProps.get("reserved-memory-capacity-per-machine-megabytes").equals("1024"));
        Assert.assertTrue("auth groups not set to in machine provisioning props",
                machineProps.get("auth-groups.value").equals("DUMMY_AUTHGROUPS"));
        Assert.assertTrue("cloud overrides does not contain expected value",
                machineProps.get("cloud-overrides-per-service").equals("envVariable=DEFAULT_OVERRIDES_ENV_VARIABLE"));
        Assert.assertTrue("cpu cores per machine should be set to 1.0",
                machineProps.get("number-of-cpu-cores-per-machine").equals("1.0"));

    }

    private void assertStatefulDeploymentIntegrity(
            final ElasticStatefulProcessingUnitConfig deploymentHolder) {

        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("service type context property not set",
                contextProperties.get("com.gs.service.type").equals("UNDEFINED"));
        Assert.assertFalse("public isolation should be set to false",
                deploymentHolder.getPublicIsolationConfig());
        Assert.assertTrue("cluster-config.mirror-service.interval-opers",
                contextProperties.get("cluster-config.mirror-service.interval-opers").equals("1000"));

        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("public isolation should be set to false",
                elasticProperties.get("elastic-machine-isolation-public-id").equals("false"));
        Assert.assertTrue("container memory capacity should be set to 128MB",
                elasticProperties.get("container.memory-capacity").equals("128"));
        Assert.assertTrue("schema should be set to partitioned-sync2backup",
                elasticProperties.get("schema").equals("partitioned-sync2backup"));

        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("scale stategy memory capacity in MB is expected to be 256MB",
                scaleStrategy.getProperties().get("memory-capacity-megabytes").equals("256"));
        Assert.assertTrue("one container per machine must be set to true",
                scaleStrategy.getProperties().get("at-most-one-container-per-machine").equals("true"));
    }

    @Test
    public void testElasticSpaceDeploymentIntegrity() throws Exception {
        final Service service = ServiceReader.readService(new File(DATAGRID_SERVICE_FILE));
        //cloud deployment assertions
        DeploymentConfig deploymentConfig = createDeploymentConfig(false, service);
        ElasticSpaceDeployment deployment =
                (ElasticSpaceDeployment) deploymentFactory.create(deploymentConfig);
        ElasticSpaceConfig deploymentHolder = deployment.create();
        assertSharedCloudDeploymentPropertiesSet(deploymentHolder);
        assertSpacePropertiesSet(deploymentHolder);

        //localcloud deployment assertions
        deploymentConfig = createDeploymentConfig(true, service);
        deployment = (ElasticSpaceDeployment) deploymentFactory.create(deploymentConfig);
        deploymentHolder = deployment.create();
        assertSharedLocalcloudProperties(deploymentHolder);
        assertSpaceLocalcloudProperties(deploymentHolder);
    }

    private void assertSpaceLocalcloudProperties(
            final ElasticSpaceConfig deploymentHolder) {
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("container memory capacity should be set to 128",
                elasticProperties.get("container.memory-capacity").equals("128"));
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("mirror-interval context property is ont set",
                contextProperties.get("cluster-config.mirror-service.interval-opers").equals("1000"));
        //assert scale strategy
        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("cpu capacity cores is expected to be equal to 0",
                scaleStrategy.getProperties().get("memory-capacity-megabytes").equals("128"));
    }

    private void assertSpacePropertiesSet(final ElasticSpaceConfig deploymentHolder) {
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("mirror-interval context property is ont set",
                contextProperties.get("cluster-config.mirror-service.interval-opers").equals("1000"));
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("schema should be set to partitioned-sync2backup",
                elasticProperties.get("schema").equals("partitioned-sync2backup"));

        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("one container per machine must be set to true",
                scaleStrategy.getProperties().get("at-most-one-container-per-machine").equals("true"));
    }

    
    @Test
    public void testElasticStatelessDeploymentIntegrity() throws Exception {
        final Service service = ServiceReader.readService(new File(STATELESS_SERVICE_FILE));
        //cloud deployment assertions
        DeploymentConfig deploymentConfig = createDeploymentConfig(false, service);
        //override dummy file
        deploymentConfig.setPackedFile(new File(STATELESS_PACKED_FILE));
        ElasticStatelessProcessingUnitDeployment deployment =
                (ElasticStatelessProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        ElasticStatelessProcessingUnitConfig deploymentHolder = deployment.create();
        assertSharedCloudDeploymentPropertiesSet(deploymentHolder);
        assertStatelessDeploymentIntegrity(deploymentHolder);

        //localcloud deployment assertions
        deploymentConfig = createDeploymentConfig(true, service);
        //override dummy file
        deploymentConfig.setPackedFile(new File(STATELESS_PACKED_FILE));
        deployment = (ElasticStatelessProcessingUnitDeployment) deploymentFactory.create(deploymentConfig);
        deploymentHolder = deployment.create();
        assertSharedLocalcloudProperties(deploymentHolder);
        assertStatelessLocalcloudProperties(deploymentHolder);
    }

    private void assertStatelessLocalcloudProperties(
            final ElasticStatelessProcessingUnitConfig deploymentHolder) {
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();
        Assert.assertTrue("container memory capacity should be set to 128",
                elasticProperties.get("container.memory-capacity").equals("128"));
        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("mirror-interval context property is ont set",
                contextProperties.get("com.gs.dummy").equals("value"));
        //assert scale strategy
        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("cpu capacity cores is expected to be equal to 0",
                scaleStrategy.getProperties().get("memory-capacity-megabytes").equals("128"));

    }

    private void assertStatelessDeploymentIntegrity(
            final ElasticStatelessProcessingUnitConfig deploymentHolder) {
        final Map<String, String> elasticProperties = deploymentHolder.getElasticProperties();

        Assert.assertTrue("container memory capacity should be set to 476MB",
                elasticProperties.get("container.memory-capacity").equals("476"));
        Assert.assertTrue("public isolation should be set to false",
                elasticProperties.get("elastic-machine-isolation-public-id").equals("false"));
        final ScaleStrategyConfig scaleStrategy = deploymentHolder.getScaleStrategy();
        Assert.assertTrue("one container per machine must be set to true",
                scaleStrategy.getProperties().get("at-most-one-container-per-machine").equals("true"));

        final Map<String, String> contextProperties = deploymentHolder.getContextProperties();
        Assert.assertTrue("service type context property not set",
                contextProperties.get("com.gs.service.type").equals("UNDEFINED"));
        Assert.assertFalse("public isolation should be set to false",
                deploymentHolder.getPublicIsolationConfig());

        Assert.assertTrue("cluster-config.mirror-service.interval-opers",
                contextProperties.get("com.gs.dummy").equals("value"));
    }
}
