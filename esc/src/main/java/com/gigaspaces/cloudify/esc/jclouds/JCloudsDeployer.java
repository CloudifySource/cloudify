package com.gigaspaces.cloudify.esc.jclouds;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
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
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
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

	private final String provider;

	private String securityGroup;

	private String keyPair;

	private String locationId;
	
	public String getSecurityGroup() {
		return securityGroup;
		
	}
	
	public void close() {
		this.context.close();
	}

	public void setSecurityGroup(final String securityGroup) {
		this.securityGroup = securityGroup;
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

		this.provider = provider;
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
			logger.info("JClouds Deployer is creating a new server with tag: "
					+ serverName + ". This may take a few minutes");
			nodes = createServersWithRetry(serverName, 1, getTemplate());
		} catch (final RunNodesException e) {
			throw new InstallerException("Failed to start Cloud server with JClouds", e);
		}
		if (nodes.isEmpty()) {
			throw new IllegalStateException("Failed to create server");

		}
		if (nodes.size() > 1) {
			throw new IllegalStateException("Created too manys servers");
		}

		return nodes.iterator().next();

	}

	   public Set<? extends NodeMetadata> createServers(final String groupName, int numberOfMachines) throws InstallerException {

	        Set<? extends NodeMetadata> nodes = null;
	        try {
	            logger.info("JClouds Deployer is creating new machines with group: "
	                    + groupName + ". This may take a few minutes");
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
	public Set<? extends NodeMetadata> createDefaultServer(final String name)
			throws RunNodesException {
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
		while(nodesIterator.hasNext()) {
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
            public boolean apply(ComputeMetadata input) {
                final NodeMetadata node = (NodeMetadata) input;
                return node.getGroup() != null &&
                       node.getGroup().equals(group);
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

			
			if (!"aws-ec2".equals(provider)) {
				this.template = builder.build();
			}
			else {
				builder.locationId(getLocationId());
				this.template = builder.build();
				
				String group = "default";
				if ((this.securityGroup != null) && (this.securityGroup.length() > 0)) {
					group = this.securityGroup;
				}
				template.getOptions().as(EC2TemplateOptions.class)
						.securityGroups(group);
				
				if ((this.keyPair != null) && (this.keyPair.length() > 0)) {
					template.getOptions().as(EC2TemplateOptions.class).keyPair(this.keyPair);
				}
			}

			// builder.options(TemplateOptions.Builder.blockUntilRunning(false));

		}

		return this.template;
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
                return input.getGroup() != null && 
                       input.getGroup().equals(group);
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

	public void setKeyPair(final String keyPair) {
		this.keyPair = keyPair;

	}

	private Set<? extends NodeMetadata> createServersWithRetry(String group, int count, Template template) throws RunNodesException {
        int retryAttempts = 0;
        boolean retry;
        
        Set<? extends NodeMetadata> nodes = null;
        
        do {
            retry = false;
            try {
                nodes = this.context.getComputeService().createNodesInGroup(group, count, template);
            } catch (final ResourceNotFoundException e) {
                if (retryAttempts < NUMBER_OF_RETRY_ATTEMPTS &&
                    e.getMessage() != null && 
                    e.getMessage().contains("The security group") &&
                    e.getMessage().contains("does not exist")) {
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

}
