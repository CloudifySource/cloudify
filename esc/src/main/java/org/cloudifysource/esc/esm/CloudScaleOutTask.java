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
package org.cloudifysource.esc.esm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.installer.esm.AbstractStartServerRunnable;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;


/************
 * A scale out task implementation, based on the Installer package
 * Implementation.
 * 
 * @author barakme
 * 
 */
public class CloudScaleOutTask extends AbstractStartServerRunnable {

	private static Logger logger = Logger.getLogger(CloudScaleOutTask.class.getName());

	private static final int WAIT_THREAD_SLEEP_MILLIS = 10000;
	private static final int WAIT_TIMEOUT_MILLIS = 360000;

	private final String machineGroup;

	private final JCloudsDeployer deployer;

	private Set<? extends NodeMetadata> servers;

	/**********
	 * Constructor.
	 * 
	 * @param installer
	 *            .
	 * @param details
	 *            The installation details, minus the server details - ip,
	 *            username and password.
	 * @param serverName
	 *            The name of the server.
	 * @param deployer
	 *            Initialized Rackspace Deployer.

	 * @throws FileNotFoundException 
	 */
	@Deprecated
	public CloudScaleOutTask(final AgentlessInstaller installer, final InstallationDetails details,
			final String serverName, final JCloudsDeployer deployer) throws FileNotFoundException {
		super(installer, details);
		this.machineGroup = serverName;
		this.deployer = deployer;
	}

	   /**********
     * Constructor.
     * 
     * @param installer
     *            .
     * @param config
     *            The installation details, minus the server details - ip,
     *            username and password.
     * @param machineGroup
     *            The name of the server.
     * @param deployer
     *            Initialized Rackspace Deployer.
     * @param lusIP
     *            The IP of the LUS/ESM that the new server should connect to.
	 * @throws FileNotFoundException 
     */
	public CloudScaleOutTask(final AgentlessInstaller installer, final CloudMachineProvisioningConfig config,
	        final String machineGroup, final JCloudsDeployer deployer, final String lusIP) throws FileNotFoundException {
	    super(installer, createInstallationDetails(config));
	    this.machineGroup = machineGroup;
	    this.deployer = deployer;
	}
	
	@Override
	public InstallationDetails[] startServers(int numberOfMachines, 
	        final long timeout, final TimeUnit unit) throws InstallerException, TimeoutException,
			InterruptedException {

		if (timeout < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		logger.info("Starting a new cloud machines with group: " + machineGroup);

		this.servers = deployer.createServers(machineGroup, numberOfMachines);

		NodeMetadata server = this.getServers().iterator().next();
		
		if (this.getDetails().getUsername() == null || this.getDetails().getUsername().isEmpty()) {
		    this.getDetails().setUsername(server.getCredentials().identity);
		}
		
		File tempFile = null;
		if (this.getDetails().getPassword() == null || this.getDetails().getPassword().isEmpty()) {
    		final String credential = server.getCredentials().credential;
    
    		if (credential == null) { // must be using an existing key file
    			if ((this.getDetails().getKeyFile() == null) || (this.getDetails().getKeyFile().length() == 0)) {
    				throw new InstallerException("Expected to receive a key " +
    						"file for authentication to new server");
    			}
    			tempFile = new File(this.getDetails().getKeyFile());
    		} else if (credential.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
    
    			FileWriter writer = null;
    			try {
    				tempFile = File.createTempFile("gs-esm-key", ".pem");
    				writer = new FileWriter(tempFile);
    				writer.write(credential);
    				this.getDetails().setKeyFile(tempFile.getAbsolutePath());
    			} catch (final IOException e) {
    				throw new InstallerException("Failed to create a temporary file for cloud server's key file", e);
    			} finally {
    				if (writer != null) {
    					try {
    						writer.close();
    					} catch (final IOException e) {
    						// ignore
    					}
    				}
    			}
    
    		} else {
    			this.getDetails().setPassword(server.getCredentials().credential);
    		}
		}

		final File keyFile = tempFile;
		
		ExecutorService exeService = Executors.newFixedThreadPool(numberOfMachines);
		try {
		    
		    List<Future<Exception>> futures = new ArrayList<Future<Exception>>();
		    
    		for (final NodeMetadata node : servers) {
    		    Future<Exception> future = exeService.submit(new Callable<Exception>() {
                    @Override
					public Exception call() {
                        
                        logServerDetails(node, keyFile);
                        
                        try {
                            waitUntilServerIsActive(node.getId(), Utils.millisUntil(end), TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            return e;
                        } catch (InterruptedException e) {
                            return e;
                        }
                        
                        return null;
                    }
                });
    		    futures.add(future);
    		}
    		
    		for (Future<Exception> future : futures) {
    		    try {
    		        Exception e = future.get();
                    if (e != null) {
                        if (e instanceof TimeoutException) {
                            throw (TimeoutException)e;
                        } 
                        if (e instanceof InterruptedException) {
                            throw (InterruptedException)e;
                        }
                        throw new InstallerException("Failed creating machines", e);
                    }
                } catch (ExecutionException e) {
                    throw new InstallerException("Failed creating machines", e);
                }
    		}
    		
		} finally {
		    exeService.shutdown();
		}

		InstallationDetails[] result = new InstallationDetails[numberOfMachines];
		int index = 0;
		for (NodeMetadata node : servers) {
		    InstallationDetails details = getDetails().clone();
		    details.setPrivateIp(node.getPrivateAddresses().iterator().next());
		    details.setPublicIp(node.getPublicAddresses().iterator().next());
		    result[index] = details;
		    index += 1;
		}
		
		return result;

	}

	/*********
	 * Periodically gets the server status from Rackspace, until the server's
	 * status changes to ACTIVE, or a timeout expires.
	 * 
	 * @param serverId
	 *            The server ID.
	 * @param milliseconds
	 * @param l
	 * @return The server status - should always be ACTIVE.
	 */
	protected void waitUntilServerIsActive(final String serverId, final long timeout, final TimeUnit unit)
			throws TimeoutException, InterruptedException {
		final long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
		NodeMetadata server;
		while (true) {
			server = deployer.getServerByID(serverId);
			if ( server != null && server.getState() == NodeState.RUNNING) {
				break;
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("Server [ " + serverId
						+ " ] has been starting up for more more than "
						+ TimeUnit.MINUTES.convert(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) + " minutes!");
			}

			if (logger.isLoggable(Level.FINE)) {
				final String serverName = server != null ? server.getState().name() : serverId;
				logger.fine("Server Status (" + serverName + ") still not active, please wait...");
			}
			Thread.sleep(WAIT_THREAD_SLEEP_MILLIS);
		}
	}

	/********
	 * Returns the last polled server object. Should only be used after the
	 * run() method has finished to execute.
	 * 
	 * @return
	 */

	private void logServerDetails(NodeMetadata server, File tempFile) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(nodePrefix(server) + "ESM Server was created.");
            if (tempFile == null) {
                logger.info(nodePrefix(server) + "Password: ***");
            } else {
                logger.info(nodePrefix(server) + "Key File: " + tempFile.getAbsolutePath());
            }

            logger.info(nodePrefix(server) + "Public IP: " + Arrays.toString(server.getPublicAddresses().toArray()));
            logger.info(nodePrefix(server) + "Private IP: " + Arrays.toString(server.getPrivateAddresses().toArray()));
            logger.info(nodePrefix(server) + "Target IP for connection: " + server.getPrivateAddresses().iterator().next());
            if (tempFile == null) {
                logger.info(nodePrefix(server) + "Connect with putty using: putty -pw " + server.getCredentials().credential + " "
                        + this.getDetails().getUsername() + "@"
                        + server.getPublicAddresses().toArray()[0]);
            } else {

                if (server.getPublicAddresses().size() > 0) {
                    logger.info(nodePrefix(server) + "Connect with putty using: putty -i " + tempFile.getAbsolutePath() + " "
                            + server.getCredentials().identity + "@"
                            + server.getPublicAddresses().toArray()[0]);
                } else {
                    logger.info(nodePrefix(server) + "Server's is starting but its public address is not available.");
                }
            }
        }
	}

	private static String nodePrefix(NodeMetadata node) {
	    return "[" + node.getId() + "] ";
	}
	
    public Set<? extends NodeMetadata> getServers() {
        return servers;
    }
	
}
