/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.CharEncoding;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudUser;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomServiceDataAware;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.ParserUtils;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.PrivateEc2ParserException;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Instance;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Volume;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.InstanceProperties;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.VolumeMapping;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.VolumeProperties;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.cloudifysource.esc.util.TarGzUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.s3.model.S3Object;

/**
 * A custom Cloud Driver to provision Amazon EC2 machines using cloud formation templates.<br />
 * This driver can still start machines the usual way using cloudify groovy templates.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class PrivateEC2CloudifyDriver extends CloudDriverSupport implements
		ProvisioningDriver, CustomServiceDataAware {

	/**
	 * Runnable to retrieve console output from a EC2 instance.
	 */
	private class EC2Console implements Runnable {

		private AmazonEC2 ec2;
		private int nbAlreadyReadLines = 0;

		private String ipAddress;
		private int agentPort;

		private String instanceId;
		private String logHeader;
		private boolean loop = true;

		public EC2Console(final String instanceId, final String ipAddress, final int agentPort)
				throws CloudProvisioningException {
			this.ipAddress = ipAddress;
			this.agentPort = agentPort;
			this.instanceId = instanceId;
			this.logHeader = "[" + instanceId + "] ";
			this.ec2 = createAmazonEC2();
		}

		@Override
		public void run() {
			if (logger.isLoggable(Level.FINER)) {
				logger.finer(logHeader + "Getting console ouput...");
			}
			while (!Thread.interrupted() && loop) {
				try {
					sleep();

					// If the agent is started, stop the tail
					if (isPortReachable(ipAddress, agentPort)) {
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest(logHeader + "Stopping the loop...");
						}
						loop = false;
					}

					// Read the console output
					final String read = this.readEc2Output();
					if (read != null) {
						// Print the console output if there is something to print
						this.printOutput(read);
					}
				} catch (final IllegalStateException e) {
					if ("Connection pool shut down".equals(e.getMessage())) {
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest(logHeader + "Getting console ouput...");
						}
						loop = false;
					}
				} catch (final Exception e) {
					logger.log(Level.WARNING, logHeader + "Error occurs when getting console output", e);
				}
			}
			if (logger.isLoggable(Level.FINER)) {
				logger.finer(logHeader + "Stop scrolling console ouput...");
			}
		}

		private String readEc2Output() {
			final GetConsoleOutputRequest request = new GetConsoleOutputRequest(instanceId);
			final GetConsoleOutputResult consoleOutput = ec2.getConsoleOutput(request);
			final String output = consoleOutput.getOutput();
			if (output != null) {
				final String x = new String(Base64.decodeBase64(output));
				return x;
			} else {
				logger.finest("[" + instanceId + "] No output yet");
			}
			return null;
		}

		private void printOutput(final String read) {
			final StringTokenizer st = new StringTokenizer(read, "\n");
			final int tokenCount = st.countTokens();

			// On linux machines, ec2 returns the full output history.
			// We have to ignore lines we already read.
			for (int i = 0; i < this.nbAlreadyReadLines; i++) {
				st.nextToken();
			}
			if (!st.hasMoreElements()) {
				logger.finest(logHeader + "No additional output");
			}
			while (st.hasMoreElements()) {
				logger.info(logHeader + st.nextElement().toString());
			}
			this.nbAlreadyReadLines = tokenCount;
		}
	}

	private static final int NB_THREADS_CONSOLE_OUTPUT = 20;
	private static final int DEFAULT_CLOUDIFY_AGENT_PORT = 7002;
	private static final int AMAZON_EXCEPTION_CODE_400 = 400;
	private static final int MAX_SERVERS_LIMIT = 200;
	private static final long WAIT_STATUS_SLEEP_TIME = 5000L;

	private static final String CLOUDIFY_ENV_SCRIPT = "cloudify_env.sh";
	private static final String PATTERN_PROPS_JSON = "\\s*\\\"[\\w-]*\\\"\\s*:\\s*([^{(\\[\"][\\w-]+)\\s*,?";
	private static final String VOLUME_PREFIX = "cloudify-storage-";

	/** Key name for amazon tag resource's name. */
	private static final String TK_NAME = "Name";

	/**
	 * Enumeration for supported 'resource-type' value used in com.amazonaws.services.ec2.model.Filter parameter.
	 */
	private static enum TagResourceType {
		INSTANCE, VOLUME;
		public String getValue() {
			return name().toLowerCase();
		}
	}

	/** Counter for ec2 instances. */
	private static AtomicInteger counter = new AtomicInteger(0);

	/** Counter for storage instances. */
	private static AtomicInteger volumeCounter = new AtomicInteger(0);

	/** Map which contains all parsed CFN template. */
	private final Map<String, PrivateEc2Template> cfnTemplatePerService = new HashMap<String, PrivateEc2Template>();

	private AmazonEC2 ec2;
	private AmazonS3Uploader amazonS3Uploader;

	/** short name of the service (i.e without applicationName). */
	private String serviceName;

	private PrivateEc2Template privateEc2Template;
	private Object cloudTemplateName;
	private String cloudName;
	private String managerCfnTemplateFileName;

	private ExecutorService debugExecutors;

	/**
	 * *****************************************************************************************************************
	 * * ***************************************************************************************************************
	 * ***
	 */

	/**
	 * Sets the custom data file for the cloud driver instance of a specific service.<br />
	 * <p>
	 * customDataFile is a simple file or a folder like the following:
	 * 
	 * <pre>
	 *   -- applicationNameFolder --- serviceName1-cfn.template
	 *                             |- serviceName2-cfn.template
	 *                             |- serviceName3-cfn.template
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param customDataFile
	 *            files or directory containing the amazon cloudformation template of a specific service
	 * 
	 */
	@Override
	public void setCustomDataFile(final File customDataFile) {
		logger.info("Received custom data file: " + customDataFile);

		final Map<String, PrivateEc2Template> map = new HashMap<String, PrivateEc2Template>();
		PrivateEc2Template mapJson = null;

		try {
			if (customDataFile.isFile()) {
				final String templateName = this.getTemplatName(customDataFile);
				logger.fine("Parsing CFN Template for service=" + templateName);
				mapJson = ParserUtils.mapJson(PrivateEc2Template.class, customDataFile);
				map.put(templateName, mapJson);
			} else {
				final File[] listFiles = customDataFile.listFiles();
				if (listFiles != null) {
					for (final File file : listFiles) {
						if (this.isTemplateFile(file)) {
							final String templateName = this.getTemplatName(file);
							logger.fine("Parsing CFN Template for service=" + templateName);

							final File pFile = this.getPropertiesFileIfExists(templateName, customDataFile.listFiles());
							if (pFile != null) {
								// Replace properties variable with values if the properties file exists
								final String templateString = this.replaceProperties(file, pFile);
								mapJson = ParserUtils.mapJson(PrivateEc2Template.class, templateString);
								map.put(templateName, mapJson);

							} else {
								mapJson = ParserUtils.mapJson(PrivateEc2Template.class, file);
								map.put(templateName, mapJson);
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "Couldn't parse the template file: " + customDataFile.getPath());
			throw new IllegalStateException(e);
		}
		this.cfnTemplatePerService.putAll(map);
	}

	private String replaceProperties(final File file, final File propertiesFile) throws IOException {
		logger.fine("Properties file=" + propertiesFile.getName());
		final Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		String templateString = FileUtils.readFileToString(file);

		final Pattern p = Pattern.compile(PATTERN_PROPS_JSON);
		Matcher m = p.matcher(templateString);
		while (m.find()) {
			final String group = m.group();
			final String group1 = m.group(1);
			if (props.containsKey(group1)) {
				final String value = props.getProperty(group1);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Replacing property " + group + " by " + value);
				}
				templateString = m.replaceFirst(group.replace(group1, value));
				m = p.matcher(templateString);
			} else {
				throw new IllegalStateException("Couldn't find property: " + group1);
			}
		}
		return templateString;
	}

	private File getPropertiesFileIfExists(final String templateName, final File[] listFiles) {
		final String filename = templateName + "-cfn.properties";
		for (final File file : listFiles) {
			if (filename.equals(file.getName())) {
				return file;
			}
		}
		return null;
	}

	private String getTemplatName(final File file) {
		String name = file.getName();
		name = name.replace("-cfn.template", "");
		return name;
	}

	private boolean isTemplateFile(final File file) {
		final String name = file.getName();
		return name.endsWith("-cfn.template");
	}

	/** Testing purpose. */
	PrivateEc2Template getCFNTemplatePerService(final String serviceName) {
		return cfnTemplatePerService.get(serviceName);
	}

	public Cloud getCloud() {
		return this.cloud;
	}

	/**
	 * *****************************************************************************************************************
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#setConfig(org.cloudifysource.dsl.cloud.Cloud,
	 * java.lang.String, boolean, java.lang.String)
	 */
	@Override
	public void setConfig(final Cloud cloud, final String cloudTemplateName, final boolean management,
			final String fullServiceName) {

		logger.fine("Running path : " + System.getProperty("user.dir"));

		this.serviceName = this.getSimpleServiceName(fullServiceName);
		this.cloudTemplateName = cloudTemplateName;
		this.cloudName = cloud.getName();
		super.setConfig(cloud, cloudTemplateName, management, fullServiceName);

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Service name : " + this.serviceName + "(" + fullServiceName + ")");
		}

		try {
			ComputeTemplate managerTemplate = this.getManagerComputeTemplate();

			// Initialize the ec2 client if the service use the CFN template
			if (management) {
				// TODO - NO VALIDATION!
				managerCfnTemplateFileName = (String) managerTemplate.getCustom().get("cfnManagerTemplate");
			} else {
				this.privateEc2Template = cfnTemplatePerService.get(this.serviceName);
				if (this.privateEc2Template == null) {
					throw new IllegalArgumentException("CFN template not found for service:" + fullServiceName);
				}
			}
			this.ec2 = this.createAmazonEC2();

			// Create s3 client
			String locationId = (String) managerTemplate.getCustom().get("s3LocationId");
			CloudUser user = this.cloud.getUser();
			this.amazonS3Uploader = new AmazonS3Uploader(user.getUser(), user.getApiKey(), locationId);

			// Setup debug console output
			final boolean debug = BooleanUtils.toBoolean((String) managerTemplate.getCustom().get("debugMode"));
			if (debug) {
				this.debugExecutors = Executors.newFixedThreadPool(NB_THREADS_CONSOLE_OUTPUT);
			}

		} catch (final CloudProvisioningException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private ComputeTemplate getManagerComputeTemplate() {
		final String managementMachineTemplate = this.cloud.getConfiguration().getManagementMachineTemplate();
		final ComputeTemplate managerTemplate =
				this.cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);
		return managerTemplate;
	}

	PrivateEc2Template getManagerPrivateEc2Template(final File cloudDirectory, final String templateFileName)
			throws PrivateEc2ParserException, IOException {
		final File file = new File(cloudDirectory, templateFileName);

		if (!file.exists()) {
			throw new IllegalArgumentException("CFN Template not found: " + file.getPath());
		}

		logger.fine("Manager cfn template: " + file.getPath());

		final String templateName = this.getTemplatName(file);
		final File pFile = new File(file.getParent(), templateName + "-cfn.properties");

		logger.fine("Searching for manager cfn properties: " + file.getPath());

		PrivateEc2Template mapJson = null;
		if (pFile.exists()) {
			// Replace properties variable with values if the properties file exists
			final String templateString = this.replaceProperties(file, pFile);
			logger.fine("The template:\n" + templateString);
			mapJson = ParserUtils.mapJson(PrivateEc2Template.class, templateString);
		} else {
			mapJson = ParserUtils.mapJson(PrivateEc2Template.class, file);
		}
		return mapJson;
	}

	/**
	 * Remove application name from the string.<br />
	 * i.e. if fullServiceName = sampleApplication.someService, it will return someService.
	 * 
	 * @param fullServiceName
	 *            A service name.
	 * @return The service name shortened by the application name.
	 */
	private String getSimpleServiceName(final String fullServiceName) {
		if (fullServiceName != null && fullServiceName.contains(".")) {
			return fullServiceName.substring(fullServiceName.lastIndexOf(".") + 1, fullServiceName.length());
		}
		return fullServiceName;
	}

	private AmazonEC2 createAmazonEC2() throws CloudProvisioningException {
		final CloudUser user = cloud.getUser();
		final AWSCredentials credentials = new BasicAWSCredentials(user.getUser(), user.getApiKey());

		final AmazonEC2 ec2 = new AmazonEC2Client(credentials);

		final String endpoint = (String) cloud.getCustom().get("endpoint");
		if (endpoint != null) {
			ec2.setEndpoint(endpoint);
		} else {
			final Region region = this.getRegion();
			ec2.setRegion(region);
		}
		return ec2;
	}

	private Region getRegion() throws CloudProvisioningException {
		final AWSEC2Instance instance = this.privateEc2Template.getEC2Instance();
		final ValueType availabilityZoneObj = instance.getProperties().getAvailabilityZone();
		if (availabilityZoneObj != null) {
			final Region region = RegionUtils.convertAvailabilityZone2Region(availabilityZoneObj.getValue());
			logger.info("Amazon ec2 region: " + region);
			return region;
		}
		throw new CloudProvisioningException("Region and/or endpoint aren't defined");
	}

	/**
	 * *****************************************************************************************************************
	 */

	/**
	 * Start machines using CFN template if provides, use JClouds otherwise.
	 * 
	 * @param locationId
	 *            the location to allocate the machine to.
	 * @param duration
	 *            Time duration to wait for the instance.
	 * @param unit
	 *            Time unit to wait for the instance.
	 * 
	 * @return The details of the started instance.
	 * 
	 * @throws TimeoutException
	 *             In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException
	 *             If a problem was encountered while starting the machine.
	 */
	@Override
	public MachineDetails startMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Stating new machine with the following thread: threadId=" + Thread.currentThread().getId()
					+ " serviceName=" + this.serviceName);
		}

		final String newName = this.createNewName(TagResourceType.INSTANCE, cloud.getProvider().getMachineNamePrefix());
		final ProvisioningContextImpl ctx =
				(ProvisioningContextImpl) new ProvisioningContextAccess().getProvisioiningContext();
		final MachineDetails md = this.createServer(this.privateEc2Template, newName, ctx, false, duration, unit);

		logger.fine("[" + md.getMachineId() + "] Cloud Server is allocated.");
		return md;
	}

	@Override
	public boolean stopMachine(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException,
			TimeoutException, InterruptedException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Stopping new machine with the following thread: threadId=" + Thread.currentThread().getId()
					+ " serviceName=" + this.serviceName
					+ " serverIp=" + serverIp);
		}

		logger.info("Stopping instance server ip = " + serverIp + "...");
		final DescribeInstancesRequest describeInstance = new DescribeInstancesRequest();
		describeInstance.withFilters(new Filter("private-ip-address", Arrays.asList(serverIp)));
		final DescribeInstancesResult describeInstances = ec2.describeInstances(describeInstance);

		final Reservation reservation = describeInstances.getReservations().get(0);
		if (reservation != null && reservation.getInstances().get(0) != null) {
			final TerminateInstancesRequest tir = new TerminateInstancesRequest();
			tir.withInstanceIds(reservation.getInstances().get(0).getInstanceId());
			final TerminateInstancesResult terminateInstances = ec2.terminateInstances(tir);

			final String instanceId = terminateInstances.getTerminatingInstances().get(0).getInstanceId();

			try {
				this.waitStopInstanceStatus(instanceId, duration, unit);
			} finally {
				// FIXME By default, cloudify doesn't delete tags. So we should keep it that way.
				// Remove instance Tags
				// if (!terminateInstances.getTerminatingInstances().isEmpty()) {
				// logger.fine("Deleting tags for instance id=" + instanceId);
				// DeleteTagsRequest deleteTagsRequest = new DeleteTagsRequest();
				// deleteTagsRequest.setResources(Arrays.asList(instanceId));
				// ec2.deleteTags(deleteTagsRequest);
				// }
			}

		} else {
			logger.warning("No instance to stop: " + reservation);
		}
		return true;
	}

	private void waitStopInstanceStatus(final String instanceId, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		while (System.currentTimeMillis() < endTime) {

			final DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
			describeRequest.withInstanceIds(instanceId);
			final DescribeInstancesResult describeInstances = ec2.describeInstances(describeRequest);

			for (final Reservation resa : describeInstances.getReservations()) {
				for (final Instance instance : resa.getInstances()) {
					final InstanceStateType state = InstanceStateType.valueOf(instance.getState().getCode());
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("instance= " + instance.getInstanceId() + " state=" + state);
					}
					switch (state) {
					case PENDING:
					case RUNNING:
					case STOPPING:
					case SHUTTING_DOWN:
						this.sleep();
						break;
					case STOPPED:
					case TERMINATED:
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest("instance (id=" + instanceId + ") was shutdown");
						}
						return;
					default:
						throw new CloudProvisioningException("Failed to stop server - Cloud reported node in "
								+ state.getName() + " state.");

					}

				}
			}
		}

		throw new TimeoutException("Stopping instace timed out (id=" + instanceId + ")");
	}

	private void sleep() {
		try {
			Thread.sleep(WAIT_STATUS_SLEEP_TIME);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private MachineDetails createServer(final PrivateEc2Template cfnTemplate, final String machineName,
			final ProvisioningContextImpl ctx, final boolean management, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {
		final Instance ec2Instance = this.createEC2Instance(cfnTemplate, ctx, management, machineName, duration, unit);

		final MachineDetails md = new MachineDetails();
		md.setMachineId(ec2Instance.getInstanceId());
		md.setPrivateAddress(ec2Instance.getPrivateIpAddress());
		md.setPublicAddress(ec2Instance.getPublicIpAddress());
		md.setAgentRunning(true);
		md.setCloudifyInstalled(true);
		return md;
	}

	private void tagEC2Instance(final Instance ec2Instance, final String ec2InstanceName,
			final AWSEC2Instance templateInstance)
			throws CloudProvisioningException {
		final List<Tag> additionalTags = Arrays.asList(new Tag(TK_NAME, ec2InstanceName));
		this.createEC2Tags(ec2Instance.getInstanceId(), templateInstance.getProperties().getTags(), additionalTags);
	}

	private void tagEC2Volumes(final String instanceId, final PrivateEc2Template cfnTemplate)
			throws CloudProvisioningException {

		final List<VolumeMapping> volumeMappings = cfnTemplate.getEC2Instance().getProperties().getVolumes();
		if (volumeMappings != null) {
			final DescribeVolumesRequest request = new DescribeVolumesRequest();
			request.withFilters(new Filter("attachment.instance-id", Arrays.asList(instanceId)));
			final DescribeVolumesResult describeVolumes = ec2.describeVolumes(request);

			for (final Volume volume : describeVolumes.getVolumes()) {
				String volumeRef = null;
				for (final VolumeMapping vMap : volumeMappings) {
					final String device = volume.getAttachments().get(0).getDevice();
					if (device.equals(vMap.getDevice().getValue())) {
						volumeRef = vMap.getVolumeId().getValue();
						break;
					}
				}
				if (volumeRef != null) {
					final AWSEC2Volume ec2Volume = cfnTemplate.getEC2Volume(volumeRef);
					final List<org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag> templateTags =
							ec2Volume == null ? null : ec2Volume
									.getProperties().getTags();
					final List<Tag> additionalTags =
							Arrays.asList(new Tag(TK_NAME, this.createNewName(TagResourceType.VOLUME, VOLUME_PREFIX)));
					this.createEC2Tags(volume.getVolumeId(), templateTags, additionalTags);
				}
			}
		}
	}

	private String createNewName(final TagResourceType resourceType, final String prefix)
			throws CloudProvisioningException {
		String newName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		while (attempts < MAX_SERVERS_LIMIT) {
			// counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;

			switch (resourceType) {
			case INSTANCE:
				newName = prefix + counter.incrementAndGet();
				break;
			case VOLUME:
				newName = prefix + volumeCounter.incrementAndGet();
				break;
			default:
				// not possible
				throw new CloudProvisioningException("ResourceType not supported");
			}

			// verifying this server name is not already used
			final DescribeTagsRequest tagRequest = new DescribeTagsRequest();
			tagRequest.withFilters(new Filter("resource-type", Arrays.asList(resourceType.getValue())));
			tagRequest.withFilters(new Filter("value", Arrays.asList(newName)));
			final DescribeTagsResult describeTags = ec2.describeTags(tagRequest);
			final List<TagDescription> tags = describeTags.getTags();
			if (tags == null || tags.isEmpty()) {
				foundFreeName = true;
				break;
			}
		}

		if (!foundFreeName) {
			throw new CloudProvisioningException("Number of servers has exceeded allowed server limit ("
					+ MAX_SERVERS_LIMIT + ")");
		}
		return newName;
	}

	private void createEC2Tags(final String resourceId,
			final List<org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag> templateTags,
			final List<Tag> additionalTags) {
		final List<Tag> tags = new ArrayList<Tag>();

		if (templateTags != null) {
			for (final org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag tag : templateTags) {
				tags.add(tag.convertToEC2Model());
			}
		}

		if (additionalTags != null) {
			tags.addAll(additionalTags);
		}

		if (!tags.isEmpty()) {
			logger.fine("Tag resourceId=" + resourceId + " tags=" + tags);
			final CreateTagsRequest ctr = new CreateTagsRequest();
			ctr.setTags(tags);
			ctr.withResources(resourceId);
			this.ec2.createTags(ctr);
		}
	}

	private Instance waitRunningInstance(final Instance ec2instance, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		while (System.currentTimeMillis() < endTime) {
			// Sleep before requesting the instance description
			// because we can get a AWS Error Code: InvalidInstanceID.NotFound if the request is too early.
			this.sleep();

			final DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
			describeRequest.setInstanceIds(Arrays.asList(ec2instance.getInstanceId()));
			final DescribeInstancesResult describeInstances = this.ec2.describeInstances(describeRequest);

			for (final Reservation resa : describeInstances.getReservations()) {
				for (final Instance instance : resa.getInstances()) {
					final InstanceStateType state = InstanceStateType.valueOf(instance.getState().getCode());
					if (logger.isLoggable(Level.FINER)) {
						logger.finer("instance= " + instance.getInstanceId() + " state=" + state);
					}
					switch (state) {
					case PENDING:
						break;
					case RUNNING:
						logger.fine("running okay...");
						return instance;
					case STOPPING:
					case SHUTTING_DOWN:
					case TERMINATED:
					case STOPPED:
					default:
						throw new CloudProvisioningException("Failed to allocate server - Cloud reported node in "
								+ state.getName() + " state. Node details: "
								+ ec2instance);

					}
				}
			}
		}

		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	private MachineDetails[] getManagementServersMachineDetails() throws CloudProvisioningException {

		final DescribeInstancesResult describeInstances = this.requestEC2InstancesManager();
		if (describeInstances == null) {
			return new MachineDetails[0];
		}
		final List<MachineDetails> mds = new ArrayList<MachineDetails>();
		for (final Reservation resa : describeInstances.getReservations()) {
			for (final Instance instance : resa.getInstances()) {
				final MachineDetails md = this.createMachineDetailsFromInstance(instance);
				mds.add(md);
			}
		}

		return mds.toArray(new MachineDetails[mds.size()]);
	}

	private DescribeInstancesResult requestEC2InstancesManager() {
		try {
			final DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.withFilters(new Filter("instance-state-name", Arrays.asList(InstanceStateType.RUNNING.getName())),
					new Filter("tag-key", Arrays.asList("Name")),
					new Filter("tag-value", Arrays.asList(cloud.getProvider().getManagementGroup() + "*")));
			final DescribeInstancesResult describeInstances = ec2.describeInstances(request);
			return describeInstances;
		} catch (final AmazonServiceException e) {
			if (e.getStatusCode() == AMAZON_EXCEPTION_CODE_400) {
				// Not found
				return null;
			} else {
				throw e;
			}
		}
	}

	private MachineDetails createMachineDetailsFromInstance(final Instance instance) throws CloudProvisioningException {
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(
				this.cloudTemplateName);

		if (template == null) {
			throw new CloudProvisioningException("Could not find template " + this.cloudTemplateName);
		}

		final MachineDetails md = new MachineDetails();
		md.setAgentRunning(false);
		md.setRemoteExecutionMode(template.getRemoteExecution());
		md.setFileTransferMode(template.getFileTransfer());
		md.setScriptLangeuage(template.getScriptLanguage());

		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);
		md.setMachineId(instance.getInstanceId());
		md.setPrivateAddress(instance.getPrivateIpAddress());
		md.setPublicAddress(instance.getPublicIpAddress());
		md.setRemoteUsername(template.getUsername());
		md.setRemotePassword(template.getPassword());
		final String availabilityZone = instance.getPlacement().getAvailabilityZone();
		md.setLocationId(RegionUtils.convertAvailabilityZone2LocationId(availabilityZone));
		md.setOpenFilesLimit(this.template.getOpenFilesLimit());

		return md;
	}

	private Instance createEC2Instance(final PrivateEc2Template cfnTemplate, final ProvisioningContextImpl ctx,
			final boolean management, final String machineName, final long duration,
			final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {

		final ComputeTemplate template = this.getManagerComputeTemplate();
		final InstanceProperties properties = cfnTemplate.getEC2Instance().getProperties();

		final String availabilityZone = properties.getAvailabilityZone() == null
				? null : properties.getAvailabilityZone().getValue();
		final Placement placement = availabilityZone == null ? null : new Placement(availabilityZone);
		final String imageId = properties.getImageId() == null ? null : properties.getImageId().getValue();
		final String instanceType = properties.getInstanceType() == null
				? null : properties.getInstanceType().getValue();
		final String keyName = properties.getKeyName() == null ? null : properties.getKeyName().getValue();
		final String privateIpAddress = properties.getPrivateIpAddress() == null
				? null : properties.getPrivateIpAddress().getValue();
		final List<String> securityGroupIds = properties.getSecurityGroupIdsAsString();
		final List<String> securityGroups = properties.getSecurityGroupsAsString();

		S3Object s3Object = null;
		try {

			String userData = null;
			if (properties.getUserData() != null) {
				// Generate ENV script for the provisioned machine
				final StringBuilder sb = new StringBuilder();
				final String script =
						management ? this.generateManagementCloudifyEnv(ctx) : this.generateCloudifyEnv(ctx);

				s3Object = this.uploadCloudDir(ctx, script, management);
				final String cloudFileS3 = this.amazonS3Uploader.generatePresignedURL(s3Object);

				String cloudFileDir = (String) template.getRemoteDirectory();
				// Remove '/' from the path if it's the last char.
				if (cloudFileDir.length() > 1 && cloudFileDir.endsWith("/")) {
					cloudFileDir = cloudFileDir.substring(0, cloudFileDir.length() - 1);
				}
				final String endOfLine = " >> /tmp/cloud.txt\n";
				sb.append("#!/bin/bash\n");
				sb.append("export TMP_DIRECTORY=/tmp").append(endOfLine);
				sb.append("export S3_ARCHIVE_FILE='" + cloudFileS3 + "'").append(endOfLine);
				sb.append("wget -q -O $TMP_DIRECTORY/cloudArchive.tar.gz $S3_ARCHIVE_FILE").append(endOfLine);
				sb.append("mkdir -p " + cloudFileDir).append(endOfLine);
				sb.append("tar zxvf $TMP_DIRECTORY/cloudArchive.tar.gz -C " + cloudFileDir).append(endOfLine);
				sb.append("rm -f $TMP_DIRECTORY/cloudArchive.tar.gz").append(endOfLine);
				sb.append("echo ").append(cloudFileDir).append("/").append(CLOUDIFY_ENV_SCRIPT).append(endOfLine);
				sb.append("chmod 755 ").append(cloudFileDir).append("/").append(CLOUDIFY_ENV_SCRIPT).append(endOfLine);
				sb.append("source ").append(cloudFileDir).append("/").append(CLOUDIFY_ENV_SCRIPT).append(endOfLine);

				sb.append(properties.getUserData().getValue());
				userData = sb.toString();
				logger.fine("Instanciate ec2 with user data:\n" + userData);
				userData = StringUtils.newStringUtf8(Base64.encodeBase64(userData.getBytes()));
			}

			List<BlockDeviceMapping> blockDeviceMappings = null;
			AWSEC2Volume volumeConfig = null;
			if (properties.getVolumes() != null) {
				blockDeviceMappings = new ArrayList<BlockDeviceMapping>(properties.getVolumes().size());
				for (final VolumeMapping volMapping : properties.getVolumes()) {
					volumeConfig = cfnTemplate.getEC2Volume(volMapping.getVolumeId().getValue());
					blockDeviceMappings.add(this.createBlockDeviceMapping(volMapping.getDevice().getValue(),
							volumeConfig));
				}
			}

			final RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
			runInstancesRequest.withPlacement(placement);
			runInstancesRequest.withImageId(imageId);
			runInstancesRequest.withInstanceType(instanceType);
			runInstancesRequest.withKeyName(keyName);
			runInstancesRequest.withPrivateIpAddress(privateIpAddress);
			runInstancesRequest.withSecurityGroupIds(securityGroupIds);
			runInstancesRequest.withSecurityGroups(securityGroups);
			runInstancesRequest.withMinCount(1);
			runInstancesRequest.withMaxCount(1);
			runInstancesRequest.withBlockDeviceMappings(blockDeviceMappings);
			runInstancesRequest.withUserData(userData);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("EC2::Instance request=" + runInstancesRequest);
			}

			final RunInstancesResult runInstances = this.ec2.runInstances(runInstancesRequest);
			if (runInstances.getReservation().getInstances().size() != 1) {
				throw new CloudProvisioningException("Request runInstace fails (request=" + runInstancesRequest + ").");
			}

			Instance ec2Instance = runInstances.getReservation().getInstances().get(0);
			ec2Instance = this.waitRunningInstance(ec2Instance, duration, unit);
			this.tagEC2Instance(ec2Instance, machineName, cfnTemplate.getEC2Instance());
			this.tagEC2Volumes(ec2Instance.getInstanceId(), cfnTemplate);

			final boolean debug = BooleanUtils.toBoolean((String) template.getCustom().get("debugMode"));
			if (debug) {
				debugExecutors.submit(new EC2Console(
						ec2Instance.getInstanceId(),
						ec2Instance.getPublicIpAddress(),
						DEFAULT_CLOUDIFY_AGENT_PORT));
			}
			this.waitRunningAgent(ec2Instance.getPublicIpAddress(), duration, unit);

			return ec2Instance;
		} finally {
			if (s3Object != null) {
				this.amazonS3Uploader.deleteS3Object(s3Object.getBucketName(), s3Object.getKey());
			}
		}
	}

	private void waitRunningAgent(final String host, final long duration, final TimeUnit unit) {
		long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		while (System.currentTimeMillis() < endTime) {
			if (this.isPortReachable(host, DEFAULT_CLOUDIFY_AGENT_PORT)) {
				logger.fine("Agent is reachable on: " + host + ":" + DEFAULT_CLOUDIFY_AGENT_PORT);
				break;
			} else {
				this.sleep();
			}
		}

	}

	private boolean isPortReachable(final String host, final int port) {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					logger.warning("Can't close port: " + host + ":" + port);
					return false;
				}
			}
		}
	}

	private S3Object uploadCloudDir(final ProvisioningContextImpl ctx, final String script, final boolean isManagement)
			throws CloudProvisioningException {
		try {
			final ComputeTemplate template = this.getManagerComputeTemplate();
			final String cloudDirectory =
					isManagement ? ((File) this.cloud.getCustom().get("###CLOUD_DIRECTORY###")).getAbsolutePath()
							: template.getAbsoluteUploadDir();
			final String s3BucketName = (String) template.getCustom().get("s3BucketName");

			// Generate env script
			final StringBuilder sb = new StringBuilder();
			sb.append("#!/bin/bash\n");
			sb.append(script);
			if (isManagement) {
				// TODO retrieve port dynamically for LUS_IP_ADDRESS
				sb.append("export LUS_IP_ADDRESS=`curl http://instance-data/latest/meta-data/local-ipv4`:4174");
			}

			// Create tmp dir
			final File createTempFile = File.createTempFile("cloudify_env", "");
			createTempFile.delete();
			// Create tmp file
			final File tmpEnvFile = new File(createTempFile, CLOUDIFY_ENV_SCRIPT);
			tmpEnvFile.deleteOnExit();
			// Write the script into the temp filedir
			FileUtils.writeStringToFile(tmpEnvFile, sb.toString(), CharEncoding.UTF_8);

			// Compress file
			logger.fine("Archive folders to upload: " + cloudDirectory + " and " + tmpEnvFile.getAbsolutePath());
			String[] sourcePaths = new String[] { cloudDirectory, tmpEnvFile.getAbsolutePath() };
			final File tarGzFile = TarGzUtils.createTarGz(sourcePaths, false);

			// Upload to S3
			final S3Object s3Object = amazonS3Uploader.uploadFile(s3BucketName, tarGzFile);
			return s3Object;
		} catch (IOException e) {
			throw new CloudProvisioningException(e);
		}
	}

	private BlockDeviceMapping createBlockDeviceMapping(final String device, final AWSEC2Volume volumeConfig)
			throws CloudProvisioningException {
		final VolumeProperties volumeProperties = volumeConfig.getProperties();
		final Integer iops = volumeProperties.getIops() == null ? null : volumeProperties.getIops();
		final Integer size = volumeProperties.getSize();
		final String snapshotId =
				volumeProperties.getSnapshotId() == null ? null : volumeProperties.getSnapshotId().getValue();
		final String volumeType =
				volumeProperties.getVolumeType() == null ? null : volumeProperties.getVolumeType().getValue();

		final EbsBlockDevice ebs = new EbsBlockDevice();
		ebs.setIops(iops);
		ebs.setSnapshotId(snapshotId);
		ebs.setVolumeSize(size);
		ebs.setVolumeType(volumeType);
		ebs.setDeleteOnTermination(true);

		final BlockDeviceMapping mapping = new BlockDeviceMapping();
		mapping.setDeviceName(device);
		mapping.setEbs(ebs);
		return mapping;
	}

	private String generateManagementCloudifyEnv(final ManagementProvisioningContext ctx)
			throws CloudProvisioningException {
		final ComputeTemplate template = new ComputeTemplate();
		// FIXME may not work on windows because of script language
		template.setScriptLanguage(ScriptLanguages.LINUX_SHELL);
		template.setRemoteDirectory("");
		try {
			final MachineDetails machineDetails = new MachineDetails();
			machineDetails.setRemoteDirectory(getManagerComputeTemplate().getRemoteDirectory());
			final MachineDetails[] mds = { machineDetails };
			// As every specific environment variables will be set with user data we don't need to generate a script per
			// management machine.
			final String[] scripts = ctx.createManagementEnvironmentScript(mds, template);
			return scripts[0];
		} catch (final FileNotFoundException e) {
			logger.log(Level.SEVERE, "Couldn't find file: ", e.getMessage());
			throw new CloudProvisioningException(e);
		}
	}

	private String generateCloudifyEnv(final ProvisioningContext ctx) throws CloudProvisioningException {
		final ComputeTemplate template = new ComputeTemplate();
		// FIXME may not work on windows because of script language
		template.setScriptLanguage(ScriptLanguages.LINUX_SHELL);
		try {
			final MachineDetails md = new MachineDetails();
			// TODO set location id in user data.
			md.setLocationId(this.getManagementServersMachineDetails()[0].getLocationId());
			final String script = ctx.createEnvironmentScript(md, template);
			return script;
		} catch (final FileNotFoundException e) {
			logger.log(Level.SEVERE, "Couldn't find file: ", e.getMessage());
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public Object getComputeContext() {
		return null;
	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.fine("DefaultCloudProvisioning: startMachine - management == " + management);

		try {
			final File cloudDirectory =
					new ProvisioningContextAccess().getManagementProvisioiningContext().getCloudFile().getParentFile();
			this.cloud.getCustom().put("###CLOUD_DIRECTORY###", cloudDirectory);
			this.privateEc2Template =
					this.getManagerPrivateEc2Template(cloudDirectory, this.managerCfnTemplateFileName);
		} catch (PrivateEc2ParserException e) {
			throw new CloudProvisioningException("Failed to read management template: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new CloudProvisioningException("Failed to read management template: " + e.getMessage(), e);
		}

		final String managementMachinePrefix = this.cloud.getProvider().getManagementGroup();
		if (org.apache.commons.lang.StringUtils.isBlank(managementMachinePrefix)) {
			throw new CloudProvisioningException(
					"The management group name is missing - can't locate existing servers!");
		}

		// first check if management already exists
		final MachineDetails[] existingManagementServers = this.getManagementServersMachineDetails();
		if (existingManagementServers.length > 0) {
			final String serverDescriptions =
					this.createExistingServersDescription(managementMachinePrefix, existingManagementServers);
			throw new CloudProvisioningException("Found existing servers matching group "
					+ managementMachinePrefix + ": " + serverDescriptions);

		}

		// launch the management machines
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		MachineDetails[] createdMachines;
		try {
			createdMachines = this.doStartManagementMachines(numberOfManagementMachines,
					endTime, unit);
		} catch (final PrivateEc2ParserException e) {
			throw new CloudProvisioningException(e);
		}
		return createdMachines;
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
		final String serverDescriptions = sb.toString();
		return serverDescriptions;
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

	private MachineDetails[] doStartManagementMachines(final int numberOfManagementMachines, final long endTime,
			final TimeUnit unit) throws TimeoutException, CloudProvisioningException, PrivateEc2ParserException {
		final ExecutorService executors = Executors.newFixedThreadPool(numberOfManagementMachines);

		@SuppressWarnings("unchecked")
		final Future<MachineDetails>[] futures = (Future<MachineDetails>[]) new Future<?>[numberOfManagementMachines];

		try {
			final PrivateEc2Template template = this.privateEc2Template;
			final String managementGroup = this.cloud.getProvider().getManagementGroup();
			final ProvisioningContextImpl ctx =
					(ProvisioningContextImpl) new ProvisioningContextAccess().getManagementProvisioiningContext();

			logger.info("ctx_threadlocal=" + ctx);

			// Call startMachine asynchronously once for each management machine
			for (int i = 0; i < numberOfManagementMachines; i++) {
				final int index = i + 1;
				futures[i] = executors.submit(new Callable<MachineDetails>() {
					@Override
					public MachineDetails call() throws Exception {
						return createServer(template, managementGroup + index, ctx, true, endTime, unit);
					}
				});

			}

			// Wait for each of the async calls to terminate.
			int numberOfErrors = 0;
			Exception firstCreationException = null;
			final MachineDetails[] createdManagementMachines = new MachineDetails[numberOfManagementMachines];
			for (int i = 0; i < createdManagementMachines.length; i++) {
				try {
					createdManagementMachines[i] = futures[i].get(endTime - System.currentTimeMillis(),
							TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e) {
					++numberOfErrors;
					logger.log(Level.SEVERE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}

				} catch (final ExecutionException e) {
					++numberOfErrors;
					logger.log(Level.SEVERE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}
				}
			}

			// In case of a partial error, shutdown all servers that did start up
			if (numberOfErrors > 0) {
				this.handleProvisioningFailure(numberOfManagementMachines, numberOfErrors, firstCreationException,
						createdManagementMachines);
			}

			return createdManagementMachines;
		} finally {
			if (executors != null) {
				executors.shutdownNow();
			}
		}
	}

	private void handleProvisioningFailure(final int numberOfManagementMachines, final int numberOfErrors,
			final Exception firstCreationException, final MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines
				+ " management machines, " + numberOfErrors
				+ " failed to start.");
		if (numberOfManagementMachines > numberOfErrors) {
			logger.severe("Shutting down the other managememnt machines");

			for (final MachineDetails machineDetails : createdManagementMachines) {
				if (machineDetails != null) {
					logger.severe("Shutting down machine: " + machineDetails);
					final TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
					terminateInstancesRequest.setInstanceIds(Arrays.asList(machineDetails.getMachineId()));
					ec2.terminateInstances(terminateInstancesRequest);
				}
			}
		}

		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(),
				firstCreationException);
	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {
		final MachineDetails[] managementServersMachineDetails = this.getManagementServersMachineDetails();
		final List<String> ids = new ArrayList<String>(managementServersMachineDetails.length);
		for (final MachineDetails machineDetails : managementServersMachineDetails) {
			ids.add(machineDetails.getMachineId());
		}
		final TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
		terminateInstancesRequest.setInstanceIds(ids);

		logger.info("Terminating management instances... " + terminateInstancesRequest);
		ec2.terminateInstances(terminateInstancesRequest);
	}

	@Override
	public String getCloudName() {
		return this.cloudName;
	}

	@Override
	public void close() {
		if (ec2 != null) {
			ec2.shutdown();
		}
		if (debugExecutors != null) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Shutting down console output executor.");
			}
			debugExecutors.shutdownNow();
		}
	}

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException,
			CloudProvisioningException {
		this.cfnTemplatePerService.clear();
	}
}
