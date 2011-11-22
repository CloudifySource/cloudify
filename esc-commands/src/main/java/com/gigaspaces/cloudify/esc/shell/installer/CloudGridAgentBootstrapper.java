package com.gigaspaces.cloudify.esc.shell.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
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

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.ConditionLatch;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.CLIException;
import com.gigaspaces.cloudify.shell.installer.ManagementWebServiceInstaller;

import com.gigaspaces.cloudify.dsl.Cloud;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.esc.esm.CloudDSLToCloudMachineProvisioningConfig;
import com.gigaspaces.cloudify.esc.esm.CloudMachineProvisioningConfig;
import com.gigaspaces.cloudify.esc.esm.CloudScaleOutTask;
import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;
import com.gigaspaces.cloudify.esc.installer.InstallationDetails;
import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.gigaspaces.cloudify.esc.jclouds.JCloudsDeployer;
import com.gigaspaces.cloudify.esc.util.Utils;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.j_spaces.kernel.Environment;

public class CloudGridAgentBootstrapper {

    private static final String MANAGEMENT_APPLICATION = ManagementWebServiceInstaller.MANAGEMENT_APPLICATION_NAME;
    
    private static final int WEBUI_PORT = 8099;

    private static final int REST_GATEWAY_PORT = 8100;

    private static final String OPERATION_TIMED_OUT = "Operation timed out";

    private static final Logger logger = Logger
            .getLogger(CloudGridAgentBootstrapper.class.getName());

    private File providerDirecotry;

    private AdminFacade adminFacade;

    private boolean verbose;

    private boolean force;
    
    private int progressInSeconds;

    public void setProviderDirectory(File providerDirecotry) {
        this.providerDirecotry = providerDirecotry;
    }

    public void setAdminFacade(AdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setProgressInSeconds(int progressInSeconds) {
        this.progressInSeconds = progressInSeconds;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void boostrapCloudAndWait(long timeout, TimeUnit timeoutUnit)
            throws InstallerException, TimeoutException, CLIException, InterruptedException {

        long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

        try {

            ShellUtils.checkNotNull("providerDirectory", providerDirecotry);
            Cloud cloudDsl = ServiceReader.getCloudFromDirectory(providerDirecotry);

            final CloudMachineProvisioningConfig config = CloudDSLToCloudMachineProvisioningConfig.convert(cloudDsl);

            String managementGroup = config.getManagementGroup();
            
            Set<? extends NodeMetadata> servers = startManagementNodes(
                    managementGroup, config,
                    Utils.millisUntil(end), TimeUnit.MILLISECONDS);

            for (NodeMetadata server : servers) {
                String ipAddress = config.isConnectedToPrivateIp() ? 
                        server.getPrivateAddresses().iterator().next() :
                        server.getPublicAddresses().iterator().next();
    
                URL restAdminUrl = new URI("http", null, ipAddress, REST_GATEWAY_PORT, null, null, null).toURL();
                URL webUIUrl = new URI("http", null, ipAddress, WEBUI_PORT, null, null, null).toURL();
                    
                // We are relying on start-management command to be run on the new machine, so 
                // everything should be up if the rest admin is up
                waitForConnection(restAdminUrl, Utils.millisUntil(end), TimeUnit.MILLISECONDS);
                
                logger.info("Rest service is available at: " + restAdminUrl);
                logger.info("Webui service is available at: " + webUIUrl);
            }
            
        } catch (IOException e) {
            throw new CLIException("bootstrap-cloud failed", e);
        } catch (URISyntaxException e) {
            throw new CLIException("bootstrap-cloud failed", e);
        } 

    }

    public void teardownCloudAndWait(long timeout, TimeUnit timeoutUnit) 
        throws InstallerException, TimeoutException, CLIException, InterruptedException {
        
        long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
        
        try {
        	
            ShellUtils.checkNotNull("providerDirectory", providerDirecotry);
            Cloud cloudDsl = ServiceReader.getCloudFromDirectory(providerDirecotry);

            final CloudMachineProvisioningConfig config = CloudDSLToCloudMachineProvisioningConfig.convert(cloudDsl);

            String managementGroup = config.getManagementGroup();
            
            destroyManagementServers(managementGroup, config, Utils.millisUntil(end), TimeUnit.MILLISECONDS);
            
        } catch (IOException e) {
            throw new CLIException("bootstrap-cloud failed", e);
        }         
        
    }
    
    private Set<? extends NodeMetadata> startManagementNodes(final String machineTag,
            final CloudMachineProvisioningConfig config, long timeout,
            TimeUnit timeoutUnit) throws InterruptedException,
            TimeoutException, CLIException {

        JCloudsDeployer deployer = null;
        try {

            long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

            deployer = getJCloudsDeployer(config);
            
            NodeMetadata server = null;

            // TODO what should be done to allow multiple managements
            logger.info("Checking if a management server named " + machineTag
                    + " is already running");
            server = deployer.getServerByTag(machineTag);
            if (server != null && server.getState() != NodeState.TERMINATED) {
                throw new CLIException("A server with tag " + machineTag
                        + " already exists");
            }

            return createManagementServers(deployer, machineTag, config, 
                    Utils.millisUntil(end), TimeUnit.MILLISECONDS);
            
        } catch (InstallerException e) {
            throw new CLIException(e);
        } catch (IOException e) {
            throw new CLIException(e);
        } finally {
            if (deployer != null) {
                deployer.destroy();
            }
        }

    }
    
    private void destroyManagementServers(
            final String machineTag,
            final CloudMachineProvisioningConfig config, long timeout,
            TimeUnit timeoutUnit) throws CLIException, InterruptedException, TimeoutException {
        
        JCloudsDeployer deployer = null;
        
        try {

            long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

            deployer = getJCloudsDeployer(config);

            NodeMetadata server = null;

            logger.info("Checking if a management server named " + machineTag + " is actually running");
            Set<? extends NodeMetadata> servers = deployer.getServers(machineTag);
            Set<? extends NodeMetadata> activeServers = Sets.filter(servers, new Predicate<NodeMetadata>() {
                public boolean apply(NodeMetadata input) {
                    return input.getState() != NodeState.TERMINATED;
                }
            });
            
            if (activeServers.isEmpty()) {
                logger.info("Could not found a server named " + machineTag);
                return;
            }

            server = activeServers.iterator().next();
            
            String ipAddress = config.isConnectedToPrivateIp() ? 
                    server.getPrivateAddresses().iterator().next() :
                    server.getPublicAddresses().iterator().next();           
            
            URL restAdminUrl = new URI("http", null, ipAddress, REST_GATEWAY_PORT, null, null, null).toURL();
            adminFacade.disconnect();
            try {
                adminFacade.connect(null, null, restAdminUrl.toString());
            } catch (CLIException e) {
                // TODO should we continue or throw an exception?
                throw e;
            }
            
            if (!force) {

                for (String application : adminFacade.getApplicationsList()) {
                    if (!application.equals(MANAGEMENT_APPLICATION)) {
                        adminFacade.uninstallApplication(application);
                    }
                }

                waitForUninstallApplications(Utils.millisUntil(end), TimeUnit.MILLISECONDS);

            }

            logger.info("Terminating cloud machines");
            
            List<String> machines = adminFacade.getMachines();
            
            deployer.shutdownMachinesWithIPs(Sets.newHashSet(machines));
            
        } catch (IOException e) {
            throw new CLIException(e);
        } catch (URISyntaxException e) {
            throw new CLIException(e);
        } finally {
            if (deployer != null) {
                deployer.destroy();
            }
        }        
        
    }
    
    private Set<? extends NodeMetadata> createManagementServers(
            final JCloudsDeployer deployer, final String machineTag,
            final CloudMachineProvisioningConfig config,
            long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, InstallerException, FileNotFoundException {

        final long end = System.currentTimeMillis() + unit.toMillis(timeout);
        
        final AgentlessInstaller installer = new AgentlessInstaller();

        // Update the logging level of jsch used by the AgentlessInstaller
        Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(config.getSshLoggingLevel());

        fixConfigRelativePaths(config);
        final CloudScaleOutTask task = new CloudScaleOutTask(installer,
                config, machineTag, deployer, null);

        InstallationDetails[] details = task.startServers(config.getNumberOfManagementMachines(), 
                Utils.millisUntil(end), TimeUnit.MILLISECONDS);

        // only one machine should try and deploy the WebUI and Rest Admin
        for (int i = 1; i < details.length; i++) {
            details[i].setNoWebServices(true);
        }
        
        StringBuilder lookupSb = new StringBuilder();
        for (InstallationDetails detail : details) {
            lookupSb.append(detail.getPrivateIp()).append(",");
        }
        lookupSb.setLength(lookupSb.length() - 1);
        
        for (InstallationDetails detail : details) {
            detail.setLocator(lookupSb.toString());
        }
        
        ExecutorService exeService = Executors.newFixedThreadPool(config.getNumberOfManagementMachines());
        BootstrapLogsFilters bootstrapLogs = new BootstrapLogsFilters(verbose);
        try {
        
            bootstrapLogs.applyLogFilters();
            
            List<Future<Exception>> futures = new ArrayList<Future<Exception>>();
            
            for (final InstallationDetails detail : details) {
                Future<Exception> future = exeService.submit(new Callable<Exception>() {
                    public Exception call() {
                        try {
                            installer.installOnMachineWithIP(detail, Utils.millisUntil(end), TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            return e;
                        } catch (InterruptedException e) {
                            return e;
                        } catch (InstallerException e) {
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
                        if (e instanceof InstallerException) {
                            throw (InstallerException)e;
                        }
                        throw new InstallerException("Failed creating machines", e);
                    }
                } catch (ExecutionException e) {
                    throw new InstallerException("Failed creating machines", e);
                }
            }
            
        } finally {
            exeService.shutdown();
            bootstrapLogs.restoreLogFilters();
        }
        
        return task.getServers();

    }
    
    private void fixConfigRelativePaths(CloudMachineProvisioningConfig config) {
    	if (config.getLocalDirectory() != null && !new File(config.getLocalDirectory()).isAbsolute()) {
    		logger.fine("Assuming " + config.getLocalDirectory() + " is in " + Environment.getHomeDirectory());
			config.setLocalDirectory(new File(Environment.getHomeDirectory(),config.getLocalDirectory()).getAbsolutePath());
    	}
	}
    
	private static JCloudsDeployer getJCloudsDeployer(CloudMachineProvisioningConfig config) throws IOException {
        JCloudsDeployer deployer = new JCloudsDeployer(config.getProvider(),
                config.getUser(), config.getApiKey());
        deployer.setMinRamMegabytes((int) config.getMachineMemoryMB());
        deployer.setImageId(config.getImageId());
        deployer.setHardwareId(config.getHardwareId());
        deployer.setLocationId(config.getLocationId());
        deployer.setSecurityGroup(config.getSecurityGroup());
        deployer.setKeyPair(config.getKeyPair());
        
        return deployer;
    }
    
    private void waitForUninstallApplications(final long timeout,
            final TimeUnit timeunit) throws InterruptedException,
            TimeoutException, CLIException {
        createConditionLatch(timeout, timeunit).waitFor(
            new ConditionLatch.Predicate() {
                @Override
                public boolean isDone() throws CLIException, InterruptedException {
                    List<String> applications = adminFacade.getApplicationsList();

                    boolean done = true;
                    
                    for (String application : applications) {
                        if (!MANAGEMENT_APPLICATION.equals(application)) {
                            done = false;
                            break;
                        }
                    }
                    
                    if (!done) {
                        logger.info("Waiting for all applications to uninstall");
                    }

                    return done;
                }
            });
    }
    
    private void waitForConnection(final URL restAdminUrl, final long timeout,
            final TimeUnit timeunit) throws InterruptedException,
            TimeoutException, CLIException {
        
        adminFacade.disconnect();

        createConditionLatch(timeout, timeunit).waitFor(
                new ConditionLatch.Predicate() {

                    @Override
                    public boolean isDone() throws CLIException,
                            InterruptedException {

                        try {
                            adminFacade.connect(null, null,
                                    restAdminUrl.toString());
                            return true;
                        } catch (CLIException e) {
                            if (verbose) {
                                logger.log(Level.INFO,
                                        "Error connecting to rest service.", e);
                            }
                        }
                        logger.log(Level.INFO, "Connecting to rest service.");
                        return false;
                    }
                });
    }

    private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
        return new ConditionLatch().timeout(timeout, timeunit)
                .pollingInterval(progressInSeconds, TimeUnit.SECONDS)
                .timeoutErrorMessage(OPERATION_TIMED_OUT).verbose(verbose);
    }

}
