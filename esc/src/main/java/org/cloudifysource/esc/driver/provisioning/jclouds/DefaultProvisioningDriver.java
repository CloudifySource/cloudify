/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.jclouds;

import com.google.common.base.Predicate;
import com.google.inject.Module;
import com.j_spaces.kernel.Environment;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.JCloudsUtils;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.KeyPair;
import org.jclouds.ec2.services.KeyPairClient;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.rest.RestContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**************
 * A jclouds-based CloudifyProvisioning implementation. Uses the JClouds Compute Context API to provision an image with
 * linux installed and ssh available. If GigaSpaces is not already installed on the new machine, this class will install
 * gigaspaces and run the agent.
 *
 * @author barakme, noak
 * @since 2.0.0
 */
public class DefaultProvisioningDriver extends BaseProvisioningDriver {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String PUBLIC_IP_REGEX = "org.cloudifysource.default-cloud-driver.public-ip-regex";
    private static final String PUBLIC_IP_CIDR = "org.cloudifysource.default-cloud-driver.public-ip-cidr";
    private static final String PRIVATE_IP_REGEX = "org.cloudifysource.default-cloud-driver.private-ip-regex";
    private static final String PRIVATE_IP_CIDR = "org.cloudifysource.default-cloud-driver.private-ip-cidr";
    private static final int CLOUD_NODE_STATE_POLLING_INTERVAL = 2000;
    private static final String DEFAULT_EC2_WINDOWS_USERNAME = "Administrator";
    private static final String EC2_API = "aws-ec2";
    private static final String VCLOUD = "vcloud";
    private static final String OPENSTACK_API = "openstack-nova";
    private static final String CLOUDSTACK = "cloudstack";
    private static final String ENDPOINT_OVERRIDE = "jclouds.endpoint";
    private static final String CLOUDS_FOLDER_PATH = Environment.getHomeDirectory() + "clouds";
    private static final int MAX_VERBOSE_IDS_LENGTH = 5;
    private static final int DEFAULT_STOP_MANAGEMENT_TIMEOUT = 15;

    // TODO: should it be volatile?
    private static ResourceBundle defaultProvisioningDriverMessageBundle;

    private String groovyFile;
    private String propertiesFile;

    private JCloudsDeployer deployer;
    private SubnetInfo privateSubnetInfo;
    private Pattern privateIpPattern;
    private SubnetInfo publicSubnetInfo;
    private Pattern publicIpPattern;

    private int stopManagementMachinesTimeoutInMinutes = DEFAULT_STOP_MANAGEMENT_TIMEOUT;

    @Override
    public void setCustomDataFile(final File customDataFile) {
        logger.info("Received custom data file: " + customDataFile);
    }

    @Override
    public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
            throws CloudProvisioningException, UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Locating management servers from file information is not supported in this cloud driver");
    }

    @Override
    public Object getComputeContext() {
        ComputeServiceContext computeContext = null;
        if (deployer != null) {
            computeContext = deployer.getContext();
        }

        return computeContext;
    }

    /**
     * 1. Provider/API name
     * 2. Authentication to the cloud
     * 3. Image IDs
     * 4. Hardware IDs
     * 5. Location IDs
     * 6. Security groups
     * 7. Key-pair names (TODO: finger-print check)
     * @param validationContext The object through which writing of validation messages is done
     * @throws CloudProvisioningException
     */
    @Override
    public void validateCloudConfiguration(
            final ValidationContext validationContext) throws CloudProvisioningException {

        // TODO : move the security groups to the Template section (instead of custom map),
        // it is now supported by jclouds.

        final String providerName = cloud.getProvider().getProvider();
        String cloudFolder = CLOUDS_FOLDER_PATH + FILE_SEPARATOR + cloud.getName();
        groovyFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.groovy";
        propertiesFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.properties";

        String apiId;
        boolean endpointRequired = false;

        try {
            validationContext.validationOngoingEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
                    getFormattedMessage("validating_provider_or_api_name", providerName));
            final ProviderMetadata providerMetadata = Providers.withId(providerName);
            final ApiMetadata apiMetadata = providerMetadata.getApiMetadata();
            apiId = apiMetadata.getId();
            validationContext.validationEventEnd(ValidationResultType.OK);
        } catch (final NoSuchElementException e) {
            // there is no jclouds Provider by that name, this could be the name of an API used in a private cloud
            try {
                final ApiMetadata apiMetadata = Apis.withId(providerName);
                apiId = apiMetadata.getId();
                endpointRequired = true;
                validationContext.validationEventEnd(ValidationResultType.OK);
            } catch (final NoSuchElementException ex) {
                validationContext.validationEventEnd(ValidationResultType.ERROR);
                throw new CloudProvisioningException(getFormattedMessage("error_provider_or_api_name_validation",
                        providerName, cloudFolder), ex);
            }
        }
        validateComputeTemplates(endpointRequired, apiId, validationContext);
    }


    @Override
    public void initDeployer(final Cloud cloud) {
        if (this.deployer != null) {
            return;
        }

        try {
            this.stopManagementMachinesTimeoutInMinutes = Utils.getInteger(cloud.getCustom().get(CloudifyConstants
                    .STOP_MANAGEMENT_TIMEOUT_IN_MINUTES), DEFAULT_STOP_MANAGEMENT_TIMEOUT);

            this.deployer = createDeployer(cloud);

            initIPFilters(cloud);

        } catch (final Exception e) {
            publishEvent("connection_to_cloud_api_failed", cloud.getProvider() .getProvider());
            throw new IllegalStateException("Failed to create cloud Deployer", e);
        }
    }

    @Override
    public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
            throws TimeoutException, CloudProvisioningException {

        logger.fine(this.getClass().getName()
                + ": startMachine, management mode: " + management);
        final long end = System.currentTimeMillis() + unit.toMillis(duration);

        if (System.currentTimeMillis() > end) {
            throw new TimeoutException("Starting a new machine timed out");
        }

        String groupName = serverNamePrefix + counter.incrementAndGet();
        logger.fine("Starting a new cloud server with group: " + groupName);
        return createServer(end, groupName, context.getLocationId());
    }

    @Override
    protected MachineDetails createServer(
            final String serverName,
            final long endTime,
            final ComputeTemplate template) throws CloudProvisioningException, TimeoutException {
        return createServer(endTime, serverName, null);
    }

    public Set<Module> setupModules() {
        return new HashSet<Module>();
    }

    private MachineDetails createServer(final long end, final String groupName,
                                        final String locationIdOverride) throws CloudProvisioningException {

        final ComputeTemplate cloudTemplate = this.cloud.getCloudCompute().getTemplates().get(
                this.cloudTemplateName);
        String locationId;
        if (locationIdOverride == null) {
            locationId = cloudTemplate.getLocationId();
        } else {
            locationId = locationIdOverride;
        }

        NodeMetadata node;
        final MachineDetails machineDetails;

        publishEvent(EVENT_STARTING_MACHINE_WITH_NAME, groupName);
        node = deployer.createServer(groupName, locationId);

        final String nodeId = node.getId();

        // At this point the machine is starting. Any error beyond this point
        // must clean up the machine

        try {
            // wait for node to reach RUNNING state
            publishEvent(EVENT_WAITING_FOR_NODE_TO_BE_AVAILABLE, groupName);
            node = waitForNodeToBecomeReady(nodeId, end);

            publishEvent(EVENT_MACHINE_STARTED, node.getName(), node.getPublicAddresses());

            machineDetails = createMachineDetailsFromNode(node);

            final FileTransferModes fileTransfer = cloudTemplate
                    .getFileTransfer();

            if (this.cloud.getProvider().getProvider().equals("aws-ec2") && fileTransfer == FileTransferModes.CIFS) {
                // Special password handling for windows on EC2
                if (machineDetails.getRemotePassword() == null) {
                    // The template did not specify a password, so we must be
                    // using the aws windows password mechanism.
                    handleEC2WindowsCredentials(end, node, machineDetails,cloudTemplate);
                }

            } else {

                // credentials required special handling.
                handleServerCredentials(machineDetails, cloudTemplate);
            }

        } catch (final Exception e) {
            // catch any exception - to prevent a cloud machine leaking.
            logger.log(Level.SEVERE, "Cloud machine was started but an error occurred during initialization. Shutting "
                    + "down machine", e);
            deployer.shutdownMachine(nodeId);
            throw new CloudProvisioningException(e);
        }

        return machineDetails;
    }

    private void handleEC2WindowsCredentials(
            final long end,
            final NodeMetadata node,
            final MachineDetails machineDetails,
            final ComputeTemplate cloudTemplate) throws FileNotFoundException,
            InterruptedException,
            TimeoutException,
            CloudProvisioningException {
        File pemFile;

        if (this.management) {
            final File localDirectory = new File(cloudTemplate.getAbsoluteUploadDir());
            pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
        } else {
            final String localDirectoryName = cloudTemplate.getLocalDirectory();
            logger.fine("local dir name is: " + localDirectoryName);
            final File localDirectory = new File(localDirectoryName);
            pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
        }

        if (!pemFile.exists()) {
            throw new FileNotFoundException("Could not find key file: " + pemFile);
        }

        String password;
        if (cloudTemplate.getPassword() == null) {
            // get the password using Amazon API
            this.publishEvent("waiting_for_ec2_windows_password", node.getId());

            final LoginCredentials credentials = new EC2WindowsPasswordHandler().getPassword(node,
                    this.deployer.getContext(), end, pemFile);
            password = credentials.getPassword();
            this.publishEvent("ec2_windows_password_retrieved", node.getId());

        } else {
            password = cloudTemplate.getPassword();
        }

        String username = cloudTemplate.getUsername();

        if (username == null) {
            username = DEFAULT_EC2_WINDOWS_USERNAME;
        }
        machineDetails.setRemoteUsername(username);
        machineDetails.setRemotePassword(password);
        machineDetails.setFileTransferMode(cloudTemplate.getFileTransfer());
        machineDetails.setRemoteExecutionMode(cloudTemplate
                .getRemoteExecution());
    }

    private NodeMetadata waitForNodeToBecomeReady(
            final String id,
            final long end) throws CloudProvisioningException, InterruptedException, TimeoutException {

        NodeMetadata node;

        while (System.currentTimeMillis() < end) {

            node = deployer.getServerByID(id);

            if (node == null) {
                logger.fine("Server Status (" + id + ") Not Found, please wait...");
                Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
                break;
            } else {
                switch (node.getStatus()) {
                    case RUNNING:
                        return node;
                    case PENDING:
                        logger.fine("Server Status (" + id + ") still PENDING, please wait...");
                        Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
                        break;
                    case TERMINATED:
                    case ERROR:
                    case UNRECOGNIZED:
                    case SUSPENDED:
                    default:
                        throw new CloudProvisioningException("Failed to allocate server - Cloud reported node in "
                                + node.getStatus().toString() + " state. Node details: " + node);
                }
            }

        }
        throw new TimeoutException("Node failed to reach RUNNING mode in time");
    }


    @Override
    public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
                                                    final TimeUnit unit)
            throws TimeoutException, CloudProvisioningException {

        final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();

        if (duration < 0) {
            throw new TimeoutException("Starting a new machine timed out");
        }
        final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

        logger.fine("Received request to start " + numberOfManagementMachines + " management machines") ;

        final String managementMachinePrefix = this.cloud.getProvider().getManagementGroup();
        if (StringUtils.isBlank(managementMachinePrefix)) {
            throw new CloudProvisioningException("The management group name is missing - " +
                    "can't locate existing servers!");
        }

        // first check if management already exists

        publishEvent(EVENT_RETRIEVE_EXISTING_MANAGEMENT_MACHINES, managementMachinePrefix);
        final MachineDetails[] existingManagementServers = getExistingManagementServers();
        if (existingManagementServers.length > 0) {
            final String serverDescriptions =
                    createExistingServersDescription(managementMachinePrefix, existingManagementServers);
            throw new CloudProvisioningException("Found existing servers matching group "
                    + managementMachinePrefix + ": " + serverDescriptions);

        } else {
            logger.fine("Did not find any existing management machines. continuing with bootstrap");
        }

        // launch the management machines
        publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
        final MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
        publishEvent(EVENT_MGMT_VMS_STARTED);
        return createdMachines;
    }

    @Override
    public boolean stopMachine(final String serverIp, final long duration,
                               final TimeUnit unit) throws CloudProvisioningException,
            TimeoutException, InterruptedException {

        boolean stopResult;

        logger.info("Received request to shutdown machine with ip " + serverIp);
        final NodeMetadata server = deployer.getServerWithIP(serverIp);
        if (server != null) {
            logger.info("Found machine : " + serverIp + "-" + server.getId() + ". Shutting it down and waiting for " +
                    "shutdown to complete");
            deployer.shutdownMachineAndWait(server.getId(), unit, duration);
            logger.info("Machine " + serverIp + "-" + server.getId() + " shutdown has finished.");
            stopResult = true;
        } else {
            logger.log(Level.SEVERE, "Received shutdown request for machine with ip " + serverIp
                    + " but this IP could not be found in the Cloud machine list");
            stopResult = false;
        }

        return stopResult;
    }

    @Override
    public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {

        initDeployer(this.cloud);

        final String managementMachinePrefix = this.cloud.getProvider().getManagementGroup();

        logger.fine("Received request to stop management machines. timeout is "
                + stopManagementMachinesTimeoutInMinutes + " minutes");

        publishEvent(EVENT_RETRIEVE_EXISTING_MANAGEMENT_MACHINES, managementMachinePrefix);
        final MachineDetails[] managementServers = getExistingManagementServers();

        if (managementServers.length == 0) {
            throw new CloudProvisioningException("Could not find any management machines for this "
                    + "cloud (management machine prefix is: " + this.serverNamePrefix + ")");
        }

        final Set<String> machineIps = new HashSet<String>();
        for (final MachineDetails machineDetails : managementServers) {
            machineIps.add(machineDetails.getPublicAddress());
        }

        publishEvent(EVENT_DESTROYING_MACHINES, machineIps.toString());
        try {
            this.deployer.shutdownMachinesByIds(managementServers, stopManagementMachinesTimeoutInMinutes);
            publishEvent(EVENT_MACHINES_DESTROYED_SUCCESSFULLY, machineIps.toString());
        } catch (final InterruptedException e) {
            throw new CloudProvisioningException(e);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.cloudifysource.esc.driver.provisioning.jclouds.ManagementLocator#getExistingManagementServers()
     */
    @Override
    public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
        final String managementMachinePrefix = this.serverNamePrefix;
        Set<? extends NodeMetadata> existingManagementServers;
        try {
            Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {

                @Override
                public boolean apply(final ComputeMetadata input) {
                    final NodeMetadata node = (NodeMetadata) input;
                    logger.finest("Found server " + node);
                    if (node.getGroup() == null) {
                        return false;
                    }
                    // only running or pending nodes are interesting
                    return (node.getStatus() == NodeMetadata.Status.RUNNING
                            || node.getStatus() == NodeMetadata.Status.PENDING)
                            && node.getGroup().toLowerCase().startsWith(managementMachinePrefix.toLowerCase());
                }
            };
            existingManagementServers = this.deployer.getServers(filter);

        } catch (final Exception e) {
            throw new CloudProvisioningException("Failed to read existing management servers: " + e.getMessage(), e);
        }

        final MachineDetails[] result = new MachineDetails[existingManagementServers.size()];
        int i = 0;
        for (final NodeMetadata node : existingManagementServers) {
            result[i] = createMachineDetailsFromNode(node);
            i++;

        }
        return result;
    }

    @Override
    protected void handleProvisioningFailure(
            final int numberOfManagementMachines,
            final int numberOfErrors,
            final Exception firstCreationException,
            final MachineDetails[] createdManagementMachines) throws CloudProvisioningException {
        logger.severe("Of the required " + numberOfManagementMachines + " management machines, " + numberOfErrors
                + " failed to start.");
        if (numberOfManagementMachines > numberOfErrors) {
            logger.severe("Shutting down the other managememnt machines");
            for (final MachineDetails machineDetails : createdManagementMachines) {
                if (machineDetails != null) {
                    logger.severe("Shutting down machine: " + machineDetails);
                    this.deployer
                            .shutdownMachine(machineDetails.getMachineId());
                }
            }
        }

        throw new CloudProvisioningException("One or more managememnt machines failed. "
                + "The first encountered error was: "        + firstCreationException.getMessage(), firstCreationException);
    }

    @Override
    public void close() {
        if (deployer != null) {
            deployer.close();
        }
    }


    private void populateIPs(final NodeMetadata node, final MachineDetails md, final ComputeTemplate template) {

        final CloudAddressResolver resolver = new CloudAddressResolver();
        final String privateAddress = resolver.getAddress(node.getPrivateAddresses(),
                node.getPublicAddresses(), privateSubnetInfo, this.privateIpPattern);
        final String publicAddress = resolver.getAddress(node.getPublicAddresses(), node.getPrivateAddresses(),
                publicSubnetInfo, this.publicIpPattern);

        md.setPrivateAddress(privateAddress);
        md.setPublicAddress(publicAddress);

    }

    private MachineDetails createMachineDetailsFromNode(final NodeMetadata node) {

        final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);

        final MachineDetails md = createMachineDetailsForTemplate(template);

        md.setCloudifyInstalled(false);
        md.setInstallationDirectory(null);
        md.setMachineId(node.getId());

        populateIPs(node, md, template);

        final String username = createMachineUsername(node, template);
        final String password = createMachinePassword(node, template);

        md.setRemoteUsername(username);
        md.setRemotePassword(password);

        // this will ensure that the availability zone is added to GSA that
        // starts on this machine.
        Location location = node.getLocation();
        if (location != null) {
            md.setLocationId(location.getId());
        }
        md.setOpenFilesLimit(template.getOpenFilesLimit());

        return md;
    }

    private String createMachineUsername(final NodeMetadata node,
                                         final ComputeTemplate template) {

        // Template configuration takes precedence.
        if (template.getUsername() != null) {
            return template.getUsername();
        }

        // Check if node returned a username
        if (node.getCredentials() != null) {
            final String serverIdentity = node.getCredentials().identity;
            if (serverIdentity != null) {
                return serverIdentity;
            }
        }

        return null;
    }

    private String createMachinePassword(final NodeMetadata node,
                                         final ComputeTemplate template) {

        // Template configuration takes precedence.
        if (template.getPassword() != null) {
            return template.getPassword();
        }

        // Check if node returned a username - some clouds support this
        // (Rackspace, for instance)
        if (node.getCredentials() != null
                && node.getCredentials().getOptionalPassword() != null) {
            if (node.getCredentials().getOptionalPassword().isPresent()) {
                return node.getCredentials().getPassword();
            }
        }

        return null;
    }

    /**
     *
     * @param cloud The cloud object that contains cerdentials.
     * @return A {@link JCloudsDeployer} object for remote cloud operations.
     * @throws IOException In case of an IO error.
     */
    public JCloudsDeployer createDeployer(final Cloud cloud) throws IOException {
        logger.fine("Creating JClouds context deployer with user: " + cloud.getUser().getUser());
        final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(
                cloudTemplateName);

        logger.fine("Cloud Template: " + cloudTemplateName + ". Details: " + cloudTemplate);
        final Properties props = new Properties();
        props.putAll(cloudTemplate.getOverrides());

        JCloudsDeployer deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
                cloud.getUser().getApiKey(), props, setupModules());

        deployer.setImageId(cloudTemplate.getImageId());
        deployer.setMinRamMegabytes(cloudTemplate.getMachineMemoryMB());
        deployer.setHardwareId(cloudTemplate.getHardwareId());
        deployer.setExtraOptions(cloudTemplate.getOptions());
        return deployer;
    }


    private void validateComputeTemplates(final boolean endpointRequired, final String apiId,
                                          final ValidationContext validationContext) throws CloudProvisioningException {

        JCloudsDeployer deployer = null;
        String templateName = "";
        String imageId = "";
        String hardwareId = "";
        String locationId = "";

        try {
            validationContext.validationEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
                    getFormattedMessage("validating_all_templates"));
            for (final Entry<String, ComputeTemplate> entry : cloud.getCloudCompute().getTemplates().entrySet()) {
                templateName = entry.getKey();
                validationContext.validationEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
                        getFormattedMessage("validating_template", templateName));
                final ComputeTemplate template = entry.getValue();
                final String endpoint = getEndpoint(template);
                if (endpointRequired && StringUtils.isBlank(endpoint)) {
                    throw new CloudProvisioningException("Endpoint not defined. Please add a \"jclouds.endpoint\""
                            + " entry in the template's overrides section");
                }

                try {
                    validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                            getFormattedMessage("validating_cloud_credentials"));
                    final Properties templateProps = new Properties();
                    final Map<String, Object> templateOverrides = template.getOverrides();
                    templateProps.putAll(templateOverrides);
                    logger.fine("Creating a new cloud deployer");
                    deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
                            cloud.getUser().getApiKey(), templateProps, setupModules());
                    logger.log(Level.FINE, "making API call");
                    deployer.getAllLocations();
                    validationContext.validationEventEnd(ValidationResultType.OK);
                } catch (Exception e) {

                    closeDeployer(deployer);
                    validationContext.validationEventEnd(ValidationResultType.ERROR);
                    throw new CloudProvisioningException(getFormattedMessage("error_cloud_credentials_validation",
                            groovyFile, propertiesFile));
                }

                imageId = template.getImageId();
                hardwareId = template.getHardwareId();
                locationId = template.getLocationId();

                deployer.setImageId(imageId);
                deployer.setHardwareId(hardwareId);
                deployer.setExtraOptions(template.getOptions());
                // TODO: check this memory validation
                // deployer.setMinRamMegabytes(template.getMachineMemoryMB());
                try {

                    validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                            getFormattedMessage("validating_image_hardware_location_combination",
                                    imageId == null ? "" : imageId, hardwareId == null ? "" : hardwareId,
                                    locationId == null ? "" : locationId));
                    // calling JCloudsDeployer.getTemplate effectively tests the above configuration through jclouds
                    deployer.getTemplate(locationId);
                    validationContext.validationEventEnd(ValidationResultType.OK);
                } catch (final Exception ex) {
                    validationContext.validationEventEnd(ValidationResultType.ERROR);
                    if (apiId.equalsIgnoreCase(OPENSTACK_API) && this.isVerboseValidation) {
                        validateLocationID(locationId);
                        validateHardwareID(hardwareId);
                        validateImageID(imageId);
                    }
                    throw new CloudProvisioningException(
                            getFormattedMessage("error_image_hardware_location_combination_validation",
                                    imageId == null ? "" : imageId,
                                    hardwareId == null ? "" : hardwareId, locationId == null ? "" : locationId,
                                    groovyFile, propertiesFile), ex);
                }

                if (isKnownAPI(apiId)) {
                    validateSecurityGroupsForTemplate(template, apiId, deployer.getContext(), validationContext);
                    validateKeyPairForTemplate(template, apiId, deployer.getContext(), validationContext);
                }
                validationContext.validationOngoingEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
                        getFormattedMessage("template_validated", templateName));
                validationContext.validationEventEnd(ValidationResultType.OK);
                closeDeployer(deployer);
            }
        } finally {
            closeDeployer(deployer);
        }
    }


    private void validateImageID(final String imageId) throws CloudProvisioningException {
        Image img = deployer.getContext().getComputeService().getImage(imageId);
        if (img == null) {
            Set<? extends Image> allImages = deployer.getAllImages();
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("line.separator"));
            int index = 0;
            for (Image image : allImages) {
                if (index > MAX_VERBOSE_IDS_LENGTH) {
                    sb.append("etc...");
                    break;
                }
                index++;
                sb.append(image.getId());
                if (image.getName() != null) {
                    sb.append(" - ").append(image.getName());
                }
                sb.append(System.getProperty("line.separator"));
            }
            throw new CloudProvisioningException(
                    getFormattedMessage("error_image_id_validation",
                            imageId == null ? "" : imageId, sb.toString()));
        }

    }

    private void validateHardwareID(final String hardwareId) throws CloudProvisioningException {
        final Set<? extends Hardware> allHardwareProfiles = deployer.getContext()
                .getComputeService().listHardwareProfiles();
        final List<String> ids = new ArrayList<String>();
        for (Hardware hardware : allHardwareProfiles) {
            if (hardware.getId().equals(hardwareId)) {
                return;
            }
            ids.add(hardware.getId());
        }
        final String message = createVerboseIdValidationMessage(ids);
        throw new CloudProvisioningException(
                getFormattedMessage("error_hardware_id_validation",
                        hardwareId == null ? "" : hardwareId, message));
    }

    private void validateLocationID(final String locationId)
            throws CloudProvisioningException {
        if (locationId != null) {
            Set<? extends Location> allLocations = deployer.getAllLocations();
            final List<String> ids = new ArrayList<String>();
            for (Location location : allLocations) {
                if (location.getId().equals(locationId)) {
                    return;
                }
                ids.add(location.getId());
            }
            String message = createVerboseIdValidationMessage(ids);
            throw new CloudProvisioningException(
                    getFormattedMessage("error_location_id_validation",
                            locationId, message));
        }
    }

    private String createVerboseIdValidationMessage(final List<String> ids) {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("line.separator"));
        int index = 1;
        for (String string : ids) {
            if (index > MAX_VERBOSE_IDS_LENGTH) {
                sb.append("etc...");
                break;
            }
            sb.append(string);
            sb.append(System.getProperty("line.separator"));
            index++;
        }
        return sb.toString();
    }


    private void validateSecurityGroupsForTemplate(final ComputeTemplate template, final String apiId,
                                                   final ComputeServiceContext computeServiceContext, final ValidationContext validationContext)
            throws CloudProvisioningException {

        String locationId = template.getLocationId();
        if (StringUtils.isBlank(locationId) && apiId.equalsIgnoreCase(OPENSTACK_API)) {
            locationId = getOpenstackLocationByHardwareId(template.getHardwareId());
        }

        if (locationId == null) {
            throw new CloudProvisioningException("locationId is missing");
        }

        Object securityGroupsObj = template.getOptions().get("securityGroupNames");
        if (securityGroupsObj == null) {
            securityGroupsObj = template.getOptions().get("securityGroups");
        }

        if (securityGroupsObj != null) {
            if (securityGroupsObj instanceof String[]) {
                final String[] securityGroupsArr = (String[]) securityGroupsObj;

                if (securityGroupsArr.length > 0) {
                    try {

                        if (securityGroupsArr.length == 1) {
                            validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                                    getFormattedMessage("validating_security_group", securityGroupsArr[0]));
                        } else {
                            validationContext.validationOngoingEvent(
                                    ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                                    getFormattedMessage("validating_security_groups",
                                            org.cloudifysource.esc.util.StringUtils.arrayToString(securityGroupsArr,
                                                    ", ")));
                        }

                        if (apiId.equalsIgnoreCase(EC2_API)) {
                            final RestContext<EC2Client, EC2AsyncClient> unwrapped = computeServiceContext.unwrap();
                            validateEc2SecurityGroups(unwrapped.getApi(), locationId, securityGroupsArr);
                        } else if (apiId.equalsIgnoreCase(OPENSTACK_API)) {
                            final RestContext<NovaApi, NovaAsyncApi> unwrapped = computeServiceContext.unwrap();
                            validateOpenstackSecurityGroups(unwrapped.getApi(), locationId,
                                    securityGroupsArr);
                        } else if (apiId.equalsIgnoreCase(CLOUDSTACK)) {
                                                        /*
                                                         * RestContext<CloudStackClient, CloudStackAsyncClient> unwrapped =
                                                         * computeServiceContext.unwrap();
                                                         * validateCloudstackSecurityGroups(unwrapped.getApi().getSecurityGroupClient(),
                                                         * aggregateAllValues(securityGroupsByRegions));
                                                         */

                        } else if (apiId.equalsIgnoreCase(VCLOUD)) {
                            // security groups not supported
                        } else {
                            // api validations not supported yet
                        }

                        validationContext.validationEventEnd(ValidationResultType.OK);
                    } catch (final CloudProvisioningException ex) {
                        validationContext.validationEventEnd(ValidationResultType.ERROR);
                        throw ex;
                    }
                }
            } else {
                // TODO : Validation not supported
            }
        }
    }

    private void validateKeyPairForTemplate(final ComputeTemplate template, final String apiId,
                                            final ComputeServiceContext computeServiceContext, final ValidationContext validationContext)
            throws CloudProvisioningException {

        String locationId = template.getLocationId();
        if (StringUtils.isBlank(locationId) && apiId.equalsIgnoreCase(OPENSTACK_API)) {
            locationId = getOpenstackLocationByHardwareId(template.getHardwareId());
        }

        if (StringUtils.isBlank(locationId)) {
            throw new CloudProvisioningException("locationId is missing");
        }

        Object keyPairObj = template.getOptions().get("keyPairName");
        if (keyPairObj == null) {
            keyPairObj = template.getOptions().get("keyPair");
        }

        if (keyPairObj != null) {
            if (!(keyPairObj instanceof String)) {
                throw new CloudProvisioningException("Invalid configuration: keyPair must be of type String");
            }

            final String keyPairString = (String) keyPairObj;
            if (StringUtils.isNotBlank(keyPairString)) {
                try {
                    validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
                            getFormattedMessage("validating_key_pair", keyPairString));

                    if (apiId.equalsIgnoreCase(EC2_API)) {
                        validateEC2KeyPair(computeServiceContext, locationId, keyPairString);
                    } else if (apiId.equalsIgnoreCase(OPENSTACK_API)) {
                        validateOpenstackKeyPair(computeServiceContext, locationId, keyPairString);
                    } else if (apiId.equalsIgnoreCase(CLOUDSTACK)) {
                                                /*
                                                 * RestContext<CloudStackClient, CloudStackAsyncClient> unwrapped =
                                                 * computeServiceContext.unwrap();
                                                 * validateCloudstackKeyPairs(unwrapped.getApi().getSSHKeyPairClient(),
                                                 * aggregateAllValues(keyPairsByRegions));
                                                 */
                    } else if (apiId.equalsIgnoreCase(VCLOUD)) {
                        // security groups not supported
                    } else {
                        // api validations not supported yet
                    }

                    validationContext.validationEventEnd(ValidationResultType.OK);

                } catch (final CloudProvisioningException ex) {
                    validationContext.validationEventEnd(ValidationResultType.ERROR);
                    throw ex;
                }
            }
        }
    }

    private void validateEC2KeyPair(final ComputeServiceContext computeServiceContext, final String locationId,
                                    final String keyPairName) throws CloudProvisioningException {
        final RestContext<EC2Client, EC2AsyncClient> unwrapped = computeServiceContext.unwrap();
        final EC2Client ec2Client = unwrapped.getApi();
        final KeyPairClient ec2KeyPairClient = ec2Client.getKeyPairServices();
        final String region = JCloudsUtils.getEC2region(ec2Client, locationId);
        final Set<KeyPair> foundKeyPairs = ec2KeyPairClient.describeKeyPairsInRegion(region, keyPairName);
        if (foundKeyPairs == null || foundKeyPairs.size() == 0 || foundKeyPairs.iterator().next() == null) {
            throw new CloudProvisioningException(getFormattedMessage("error_key_pair_validation", keyPairName,
                    groovyFile, propertiesFile));
        }
    }

    private void validateOpenstackKeyPair(final ComputeServiceContext computeServiceContext, final String locationId,
                                          final String keyPairName) throws CloudProvisioningException {
        final RestContext<NovaApi, NovaAsyncApi> unwrapped = computeServiceContext.unwrap();
        final KeyPairApi keyPairApi = unwrapped.getApi().getKeyPairExtensionForZone(locationId).get();
        final Predicate<org.jclouds.openstack.nova.v2_0.domain.KeyPair> keyPairNamePredicate =
                org.jclouds.openstack.nova.v2_0.predicates.KeyPairPredicates.nameEquals(keyPairName);
        if (!keyPairApi.list().anyMatch(keyPairNamePredicate)) {
            throw new CloudProvisioningException(getFormattedMessage("error_key_pair_validation", keyPairName,
                    groovyFile, propertiesFile));
        }
    }

    private boolean isKnownAPI(final String apiName) {
        boolean supported = false;

        if (apiName.equalsIgnoreCase(EC2_API)
                || apiName.equalsIgnoreCase(OPENSTACK_API)) {
            // || apiName.equalsIgnoreCase(VCLOUD)
            // || apiName.equalsIgnoreCase(CLOUDSTACK)) {
            supported = true;
        }

        return supported;
    }

    private void validateEc2SecurityGroups(final EC2Client ec2Client, final String locationId,
                                           final String[] securityGroupsInRegion) throws CloudProvisioningException {

        final String region = JCloudsUtils.getEC2region(ec2Client, locationId);
        final org.jclouds.ec2.services.SecurityGroupClient ec2SecurityGroupsClient =
                ec2Client.getSecurityGroupServices();
        final Set<String> missingSecurityGroups = new HashSet<String>();

        for (final String securityGroupName : securityGroupsInRegion) {
            final Set<org.jclouds.ec2.domain.SecurityGroup> foundGroups =
                    ec2SecurityGroupsClient.describeSecurityGroupsInRegion(region, securityGroupName);
            if (foundGroups == null || foundGroups.size() == 0 || foundGroups.iterator().next() == null) {
                missingSecurityGroups.add(securityGroupName);
            }
        }

        if (missingSecurityGroups.size() == 1) {
            throw new CloudProvisioningException(getFormattedMessage("error_security_group_validation",
                    missingSecurityGroups.iterator().next(), groovyFile, propertiesFile));
        } else if (missingSecurityGroups.size() > 1) {
            throw new CloudProvisioningException(getFormattedMessage("error_security_groups_validation",
                    Arrays.toString(missingSecurityGroups.toArray()), groovyFile, propertiesFile));
        }
    }

    private void validateOpenstackSecurityGroups(final NovaApi novaApi, final String region,
                                                 final String[] securityGroupsInRegion) throws CloudProvisioningException {

        final Set<String> missingSecurityGroups = new HashSet<String>();
        final SecurityGroupApi securityGroupApi = novaApi.getSecurityGroupExtensionForZone(region).get();

        for (final String securityGroupName : securityGroupsInRegion) {
            final Predicate<org.jclouds.openstack.nova.v2_0.domain.SecurityGroup> securityGroupNamePredicate =
                    org.jclouds.openstack.nova.v2_0.predicates.SecurityGroupPredicates.nameEquals(securityGroupName);
            if (!securityGroupApi.list().anyMatch(securityGroupNamePredicate)) {
                missingSecurityGroups.add(securityGroupName);
            }
        }

        if (missingSecurityGroups.size() == 1) {
            throw new CloudProvisioningException(getFormattedMessage("error_security_group_validation",
                    missingSecurityGroups.iterator().next(), groovyFile, propertiesFile));
        } else if (missingSecurityGroups.size() > 1) {
            throw new CloudProvisioningException(getFormattedMessage("error_security_groups_validation",
                    Arrays.toString(missingSecurityGroups.toArray()), groovyFile, propertiesFile));
        }
    }

    private String getEndpoint(final ComputeTemplate template) {
        String endpoint = null;

        final Map<String, Object> templateOverrides = template.getOverrides();
        if (templateOverrides != null && templateOverrides.size() > 0) {
            endpoint = (String) templateOverrides.get(ENDPOINT_OVERRIDE);
        }

        return endpoint;
    }

    private void closeDeployer(final JCloudsDeployer jcloudsDeployer) {
        if (jcloudsDeployer != null) {
            logger.fine("Attempting to close cloud deployer");
            jcloudsDeployer.close();
            logger.fine("Cloud deployer closed");
        }
    }

    private String getOpenstackLocationByHardwareId(final String hardwareId) {
        String region = "";
        if (!hardwareId.contains("/")) {
            logger.info("HardwareId is: " + hardwareId + ". It must be formatted "
                    + "as region / profile id");
            throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
                    + "as region / profile id");
        }

        region = StringUtils.substringBefore(hardwareId, "/");
        if (StringUtils.isBlank(region)) {
            logger.info("HardwareId " + hardwareId + " is missing the region name. It must be formatted "
                    + "as region / profile id");
            throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
                    + "as region / profile id");
        }

        logger.fine("region: " + region);
        return region;
    }

    /**
     * returns the message as it appears in the DefaultProvisioningDriver message bundle.
     *
     * @param msgName
     *            the message key as it is defined in the message bundle.
     * @param arguments
     *            the message arguments
     * @return the formatted message according to the message key.
     */
    protected String getFormattedMessage(final String msgName, final Object... arguments) {
        return getFormattedMessage(getDefaultProvisioningDriverMessageBundle(), msgName, arguments);
    }

    /**
     * Returns the message bundle of this cloud driver.
     *
     * @return the message bundle of this cloud driver.
     */
    protected static ResourceBundle getDefaultProvisioningDriverMessageBundle() {
        if (defaultProvisioningDriverMessageBundle == null) {
            defaultProvisioningDriverMessageBundle = ResourceBundle.getBundle("DefaultProvisioningDriverMessages",
                    Locale.getDefault());
        }
        return defaultProvisioningDriverMessageBundle;
    }

    private void initIPFilters(final Cloud cloud) {
        final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(
                cloudTemplateName);

        final String privateCidr =
                (String) template.getCustom().get(PRIVATE_IP_CIDR);
        if (!StringUtils.isBlank(privateCidr)) {
            this.privateSubnetInfo = new SubnetUtils(privateCidr).getInfo();
        }

        final String privateRegex =
                (String) template.getCustom().get(PRIVATE_IP_REGEX);
        if (!StringUtils.isBlank(privateRegex)) {
            this.privateIpPattern = Pattern.compile(privateRegex);
        }

        final String publicCidr =
                (String) template.getCustom().get(PUBLIC_IP_CIDR);
        if (!StringUtils.isBlank(publicCidr)) {
            this.publicSubnetInfo = new SubnetUtils(publicCidr).getInfo();
        }

        final String publicRegex =
                (String) template.getCustom().get(PUBLIC_IP_REGEX);
        if (!StringUtils.isBlank(publicRegex)) {
            this.publicIpPattern = Pattern.compile(publicRegex);
        }

    }

    private String createExistingServersDescription(final String managementMachinePrefix,
                                                    final MachineDetails[] existingManagementServers) {
        logger.info("Found existing servers matching the name: " + managementMachinePrefix);
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final MachineDetails machineDetails : existingManagementServers) {
            final String existingManagementServerDescription = createManagementServerDescription(machineDetails);
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("[").append(existingManagementServerDescription).append("]");
        }
        return sb.toString();
    }

    private String createManagementServerDescription(final MachineDetails machineDetails) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Machine ID: ").append(machineDetails.getMachineId());
        if (machineDetails.getPublicAddress() != null) {
            sb.append(", Public IP: ").append(machineDetails.getPublicAddress());
        }

        if (machineDetails.getPrivateAddress() != null) {
            sb.append(", Private IP: ").append(machineDetails.getPrivateAddress());
        }

        return sb.toString();
    }



}
