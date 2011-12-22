package com.gigaspaces.cloudify.esc.jclouds;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.rest.ResourceNotFoundException;

import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Module;

/************
 * A JClouds based deployer that creates and queries JClouds compliant servers.
 * All of the JClouds features used in the cloud ESM Machine Provisioning are
 * called from this class.
 * 
 * @author barakme
 * 
 */
public class JCloudsDeployer {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(JCloudsDeployer.class.getName());

	private static final int DEFAULT_MIN_RAM_MB = 0;
	private static final String DEFAULT_IMAGE_ID_RACKSPACE = "51";

	private static final long RETRY_SLEEP_TIMEOUT_IN_MILLIS = 5000;
	private static final int NUMBER_OF_RETRY_ATTEMPTS = 2;

	private int minRamMegabytes = DEFAULT_MIN_RAM_MB;
	private String imageId = DEFAULT_IMAGE_ID_RACKSPACE;

	private Template template;

	private final ComputeServiceContext context;

	private final ExecutorService exe;

	private String hardwareId;

	private String locationId;

	private Map<String, Object> extraOptions;

	
	public void close() {
		this.context.close();
	}

	
	public String getHardwareId() {
		return hardwareId;
	}

	public void setHardwareId(final String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public void setLocationId(final String locationId) {
		this.locationId = locationId;
	}

	public String getLocationId() {
		return locationId;
	}

	/********
	 * .
	 * 
	 * @param provider
	 *            .
	 * @param account
	 *            .
	 * @param key
	 *            .
	 * @throws IOException .
	 */
	public JCloudsDeployer(final String provider, final String account, final String key) throws IOException {

		this(provider, account, key, new Properties());
	}

	/*************
	 * .
	 * 
	 * @param provider
	 *            .
	 * @param account
	 *            .
	 * @param key
	 *            .
	 * @param overrides
	 *            .
	 * @throws IOException .
	 */
	public JCloudsDeployer(final String provider, final String account, final String key, final Properties overrides)
			throws IOException {

		final Set<Module> wiring = new HashSet<Module>();
		this.context = new ComputeServiceContextFactory().createContext(provider, account, key, wiring, overrides);

		exe = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());

	}

	/**********
	 * Starts up a server based on the deployer's template, and returns its meta
	 * data. The server may not have started yet when this call returns.
	 * 
	 * @param serverName
	 *            server name.
	 * @return the new server meta data.
	 * @throws InstallerException
	 */
	public NodeMetadata createServer(final String serverName) throws InstallerException {

		Set<? extends NodeMetadata> nodes = null;
		try {
			logger.fine("Cloudify Deployer is creating a new server with tag: " + serverName
					+ ". This may take a few minutes");
			nodes = createServersWithRetry(serverName, 1, getTemplate());
		} catch (final RunNodesException e) {
			throw new InstallerException("Failed to start Cloud server", e);
		}
		if (nodes.isEmpty()) {
			throw new IllegalStateException("Failed to create server");

		}
		if (nodes.size() > 1) {
			throw new IllegalStateException("Created too manys servers");
		}

		return nodes.iterator().next();

	}

	public Set<? extends NodeMetadata> createServers(final String groupName, int numberOfMachines)
			throws InstallerException {

		Set<? extends NodeMetadata> nodes = null;
		try {
			logger.fine("JClouds Deployer is creating new machines with group: " + groupName
					+ ". This may take a few minutes");
			nodes = createServersWithRetry(groupName, numberOfMachines, getTemplate());
		} catch (final RunNodesException e) {
			throw new InstallerException("Failed to start Cloud server with JClouds", e);
		}
		if (nodes.isEmpty()) {
			throw new IllegalStateException("Failed to create machines");

		}
		if (nodes.size() > numberOfMachines) {
			throw new IllegalStateException("Created too manys machines");
		}

		return nodes;

	}

	/********
	 * Creates a server with the given name and template.
	 * 
	 * @param serverName
	 *            the server name.
	 * @param template
	 *            the template.
	 * @return the server meta data.
	 * @throws RunNodesException .
	 */
	public NodeMetadata createServer(final String serverName, final Template template) throws RunNodesException {

		final Set<? extends NodeMetadata> nodes = createServersWithRetry(serverName, 1, template);

		if (nodes.isEmpty()) {
			return null;
		}
		if (nodes.size() != 1) {
			throw new IllegalStateException();
		}
		return nodes.iterator().next();

	}

	/*********
	 * Cleans up the deployer's resources.
	 */
	@PreDestroy
	public void destroy() {
		context.close();
		exe.shutdown();
	}

	/********
	 * Useful testing method for cloud tests. In production, you should use a
	 * template.
	 * 
	 * @param name
	 *            the server name.
	 * @return the node meta data.
	 * @throws RunNodesException .
	 */
	public Set<? extends NodeMetadata> createDefaultServer(final String name) throws RunNodesException {
		return this.context.getComputeService().createNodesInGroup(name, 1);
	}

	public Set<? extends Image> getAllImages() {
		return this.context.getComputeService().listImages();
	}

	public Set<? extends ComputeMetadata> getAllServers() {
		return this.context.getComputeService().listNodes();

	}

	public ComputeServiceContext getContext() {
		return context;
	}

	public String getImageId() {
		return imageId;
	}

	public int getMinRamMegabytes() {
		return minRamMegabytes;
	}

	/*******
	 * Queries the cloud for a single server that matches the given critetia. If
	 * more then one is returned, throws an exception.
	 * 
	 * @param filter
	 *            the query filter.
	 * @return the node meta data, or null if no match is found.
	 */
	public NodeMetadata getServer(final Predicate<ComputeMetadata> filter) {
		final Set<? extends NodeMetadata> nodes = getServers(filter);
		final Set<NodeMetadata> runningNodes = new HashSet<NodeMetadata>();
		Iterator<? extends NodeMetadata> nodesIterator = nodes.iterator();
		while (nodesIterator.hasNext()) {
			NodeMetadata node = nodesIterator.next();
			if (node.getState() != NodeState.TERMINATED) {
				runningNodes.add(node);
			}
		}
		if (runningNodes.isEmpty()) {
			return null;
		}
		if (runningNodes.size() != 1) {
			throw new IllegalStateException("runningNodes.size() == " + runningNodes.size() + " != 1");
		}
		return runningNodes.iterator().next();
	}

	/********
	 * Looks for a server with an ID that matches the given ID.
	 * 
	 * @param serverID
	 *            the server ID.
	 * @return the node meta data, or null.
	 */
	public NodeMetadata getServerByID(final String serverID) {
		final Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {

			@Override
			public boolean apply(final ComputeMetadata compute) {
				return compute.getId().equals(serverID);
			}

		};

		return getServer(filter);

	}

	/*********
	 * Queries for a server whose name STARTS WITH the given name. Note that
	 * underscores in the name ('_') are replaced with blanks.
	 * 
	 * @param serverName
	 *            the server name.
	 * @return
	 */
	public NodeMetadata getServerByName(final String serverName) {
		final String adaptedServerName = serverName.replace("_", "") + "-";
		final Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {

			@Override
			public boolean apply(final ComputeMetadata compute) {
				if (compute.getName() == null) {
					return false;
				}
				return compute.getName().startsWith(adaptedServerName);
			}

		};

		return getServer(filter);

	}

	/*******************
	 * Returns all nodes that match the given criteria.
	 * 
	 * @param filter
	 *            the filter criteria.
	 * @return the nodes.
	 */
	public Set<? extends NodeMetadata> getServers(final Predicate<ComputeMetadata> filter) {
		return this.context.getComputeService().listNodesDetailsMatching(filter);
	}

	/*******************
	 * Returns all nodes that match the group provided
	 * 
	 * @param group
	 *            the group.
	 * @return the nodes.
	 */
	public Set<? extends NodeMetadata> getServers(final String group) {
		return getServers(new Predicate<ComputeMetadata>() {

			@Override
			public boolean apply(ComputeMetadata input) {
				final NodeMetadata node = (NodeMetadata) input;
				return node.getGroup() != null && node.getGroup().equals(group);
			}
		});

	}

	/***********
	 * Returns a server whose private or public IPs contain the given IP.
	 * 
	 * @param ip
	 *            the IP to look for.
	 * @return the node meta data, or null.
	 */
	public NodeMetadata getServerWithIP(final String ip) {
		final Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {

			@Override
			public boolean apply(final ComputeMetadata compute) {
				final NodeMetadata node = (NodeMetadata) compute;
				if (node.getPrivateAddresses().contains(ip)) {
					return true;
				}

				return node.getPublicAddresses().contains(ip);

			}

		};

		return getServer(filter);

	}

	/************
	 * Returns the default template used to create new servers. Note that if the
	 * template has not been instantiated yet, it will be done in this call.
	 * 
	 * @return the template.
	 */
	public Template getTemplate() {
		if (this.template == null) {

			logger.fine("Creating Cloud Template. This may take a few seconds");
			
			final TemplateBuilder builder = this.context.getComputeService().templateBuilder();
			if ((this.imageId != null) && (this.imageId.length() > 0)) {
				builder.imageId(this.imageId);
			}

			if (this.minRamMegabytes > 0) {
				builder.minRam(minRamMegabytes);
			}

			if ((this.hardwareId != null) && (hardwareId.length() > 0)) {
				builder.hardwareId(hardwareId);
			}

			// this is usually a remote call, and may take a while to return.
			this.template = builder.build();

			handleExtraOptions();
			logger.fine("Cloud Template is ready for use.");
		}

		return this.template;
	}

	private void handleExtraOptions() {
		if (this.extraOptions != null) {
			// use reflection to set extra options
			Set<Entry<String, Object>> optionEntries = this.extraOptions.entrySet();
			TemplateOptions templateOptions = template.getOptions();

			for (Entry<String, Object> entry : optionEntries) {
				final String entryKey = entry.getKey();
				final Object entryValue = entry.getValue();
				if (entryValue == null) {
					handleNullValueTemplateOption(optionEntries, templateOptions, entry, entryKey, entryValue);
				} else if (List.class.isAssignableFrom(entryValue.getClass())) {
					handleListParameterOption(templateOptions, entryKey, entryValue);
				} else {
					handleSingleParameterOption(templateOptions, entryKey, entryValue);
				}

			}
		}
	}

	private void handleListParameterOption(TemplateOptions templateOptions, final String entryKey,
			final Object entryValue) {
		// first check for a single arg option with a list
		// parameter
		Method m = null;
		try {
			m = templateOptions.getClass().getMethod(entryKey, entryValue.getClass());
		} catch (SecurityException e) {
			throw new IllegalArgumentException(
					"Error while loo king for method to match option: " + entryKey
							+ " with option value: " + entryValue + ". Error was: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			// ignore
		}

		if (m != null) {
			// found a relevant method
			handleSingleParameterOption(templateOptions, entryKey, entryValue);
		} else {
			// no method accepts a list - try for a method that
			// takes a
			// parameter for each list entry
			@SuppressWarnings("unchecked")
			List<Object> paramList = (List<Object>) entryValue;
			Object[] paramArray = paramList.toArray();
			Class<?>[] classArray = new Class<?>[paramArray.length];
			for (int i = 0; i < classArray.length; i++) {
				classArray[i] = paramArray[i].getClass();
			}
			
			try {
				Method[] ms = templateOptions.getClass().getMethods();
				System.out.println(ms);
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
			try {
				m = templateOptions.getClass().getMethod(entryKey, classArray);
			} catch (SecurityException e) {
				throw new IllegalArgumentException("Error while looking for method to match option: "
						+ entryKey + " with option value: " + entryValue + ". Error was: "
						+ e.getMessage(), e);
			} catch (NoSuchMethodException e) {
				// ignore
			}

			if (m == null) {
				throw new IllegalArgumentException(
						"Could not find a matching method to set template option: " + entryKey
								+ " with the following values: " + paramList);
			} else {
				try {
					m.invoke(templateOptions, paramArray);
				} catch (Exception e) {
					throw new IllegalArgumentException("Failed to set option: " + entryKey
							+ " by invoking method: " + m + " with value: " + entryValue
							+ ". Error was: " + e.getMessage(), e);
				}
			}

		}
	}

	private void handleSingleParameterOption(TemplateOptions templateOptions, final String entryKey,
			final Object entryValue) {
		Method m = null;
		try {
			m = templateOptions.getClass().getMethod(entryKey, entryValue.getClass());
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Error while looking for method to match option: " + entryKey
					+ " with option value: " + entryValue + ". Error was: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			// ignore
		}
		if (m == null) {
			throw new IllegalArgumentException("Could not find a method matching option: " + entryKey + " with value: "
					+ entryValue);
		}

		try {
			m.invoke(templateOptions, entryValue);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to set option: " + entryKey + " by invoking method: " + m
					+ " with value: " + entryValue + ". Error was: " + e.getMessage(), e);
		}
	}

	private void handleNullValueTemplateOption(Set<Entry<String, Object>> optionEntries,
			TemplateOptions templateOptions, Entry<String, Object> entry, final String entryKey, final Object entryValue) {
		// first look for no arg method
		Method m = null;
		try {
			m = optionEntries.getClass().getMethod(entryKey);
			// got the method
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Error while looking for method to match template option: " + entryKey,
					e);
		} catch (NoSuchMethodException e) {
			// ignore - method was not found
		}

		if (m != null) {
			// Found a no-arg method for this option
			try {
				// invoke with no args
				m.invoke(templateOptions);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to set template option with name: " + entryKey
						+ " to value: " + entryValue, e);

			}
		} else {
			// look for a matching method with a single argument
			try {
				m = optionEntries.getClass().getMethod(entryKey, Object.class);
				// got the method
			} catch (SecurityException e) {
				throw new IllegalArgumentException("Error while looking for method to match template option: "
						+ entryKey, e);
			} catch (NoSuchMethodException e) {
				// ignore - method was not found
			}

			if (m != null) {
				try {
					//
					m.invoke(templateOptions, (Object) null);
				} catch (Exception e) {
					throw new IllegalArgumentException("Failed to set template option with name: " + entryKey
							+ " to value: " + entryValue, e);
				}
			} else {
				throw new IllegalArgumentException("Could not find a method matching template option: "
						+ entry.getKey());
			}

		}
	}

	public void setImageId(final String imageId) {
		this.imageId = imageId;
	}

	public void setMinRamMegabytes(final int minRamMegabytes) {
		this.minRamMegabytes = minRamMegabytes;
	}

	/* CHECKSTYLE:OFF */
	public void setMinRamMegabytes(final String minRamMegabytes_str) {
		if ((minRamMegabytes_str != null) && (minRamMegabytes_str.length() > 0)) {
			setMinRamMegabytes(Integer.parseInt(minRamMegabytes_str));
		}
	}

	/* CHECKSTYLE:ON */

	/******
	 * Shuts down a server with the given ID.
	 * 
	 * @param serverId
	 *            the server ID.
	 */
	public void shutdownMachine(final String serverId) {
		this.context.getComputeService().destroyNode(serverId);
	}

	public void shutdownMachineGroup(final String group) {
		this.context.getComputeService().destroyNodesMatching(new Predicate<NodeMetadata>() {

			@Override
			public boolean apply(NodeMetadata input) {
				return input.getGroup() != null && input.getGroup().equals(group);
			}
		});
	}

	public void shutdownMachinesWithIPs(final Set<String> IPs) {
		this.context.getComputeService().destroyNodesMatching(new Predicate<NodeMetadata>() {

			@Override
			public boolean apply(NodeMetadata input) {
				if (!input.getPrivateAddresses().isEmpty()) {
					String ip = input.getPrivateAddresses().iterator().next();
					return IPs.contains(ip);
				}
				return false;
			}
		});
	}

	/*********
	 * Returns a server with a tag that equals the given tag. Note that
	 * comparison is also performed with the given tag with underscores removed.
	 * 
	 * @param tag
	 *            the tag to look for.
	 * @return the node meta data, or null.
	 */
	public NodeMetadata getServerByTag(final String tag) {
		final Predicate<ComputeMetadata> filter = new Predicate<ComputeMetadata>() {

			@Override
			public boolean apply(final ComputeMetadata compute) {
				final NodeMetadata node = (NodeMetadata) compute;
				if (node.getGroup() == null) {
					return false;
				}
				if (node.getGroup().equals(tag)) {
					return true;
				}
				if (node.getGroup().equals(tag.replace("_", ""))) {
					return true;
				}
				return false;
			}

		};

		return getServer(filter);
	}


	private Set<? extends NodeMetadata> createServersWithRetry(String group, int count, Template template)
			throws RunNodesException {
		int retryAttempts = 0;
		boolean retry;

		Set<? extends NodeMetadata> nodes = null;

		do {
			retry = false;
			try {
				nodes = this.context.getComputeService().createNodesInGroup(group, count, template);
			} catch (final ResourceNotFoundException e) {
				if (retryAttempts < NUMBER_OF_RETRY_ATTEMPTS && e.getMessage() != null
						&& e.getMessage().contains("The security group") && e.getMessage().contains("does not exist")) {
					try {
						Thread.sleep(RETRY_SLEEP_TIMEOUT_IN_MILLIS);
					} catch (InterruptedException e1) {
						/* do nothing */
					}
					retryAttempts += 1;
					retry = true;
					continue;
				} else {
					throw e;
				}
			}
		} while (retry);

		return nodes;
	}

	public Map<String, Object> getExtraOptions() {
		return extraOptions;
	}

	public void setExtraOptions(Map<String, Object> extraOptions) {
		this.extraOptions = extraOptions;
	}

}
