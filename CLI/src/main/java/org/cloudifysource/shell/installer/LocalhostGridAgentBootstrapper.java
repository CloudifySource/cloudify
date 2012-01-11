package org.cloudifysource.shell.installer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.Constants;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.AgentGridComponent;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.vm.VirtualMachineAware;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;
import org.cloudifysource.dsl.utils.ServiceUtils;
import com.gigaspaces.grid.gsa.GSA;
import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

public class LocalhostGridAgentBootstrapper {

	// isolate localcloud from default lookup settings
	public static final String LOCALCLOUD_LOOKUPGROUP = "localcloud";
	private static final int DEFAULT_LUS_PORT = net.jini.discovery.Constants.getDiscoveryPort();
	private static final int LOCALCLOUD_LUS_PORT = DEFAULT_LUS_PORT +2;
	
	private static final String LOOPBACK_ADDR = "127.0.0.1";
	
	private static final String LUS_PORT_CONTEXT_PROPERTY = "com.sun.jini.reggie.initialUnicastDiscoveryPort";
	private static final String AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT = "-Dcom.gs.agent.auto-shutdown-enabled=true"; 
	private static final int WAIT_AFTER_ADMIN_CLOSED_MILLIS = 10*1000;
	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out waiting for the agent to start";
	private static final int GSA_MEMORY_IN_MB = 128;
	private static final int LUS_MEMORY_IN_MB = 128;
	private static final int GSM_MEMORY_IN_MB = 128;
	private static final int ESM_MEMORY_IN_MB = 128;
	private static final int REST_MEMORY_IN_MB = 128; // we don't have wars that big
	private static final int MANAGEMENT_SPACE_MEMORY_IN_MB = 64;
	private static final int REST_PORT = 8100;
	private static final String REST_FILE = "tools" + File.separator + "rest" + File.separator + "rest.war";
	private static final String REST_NAME = "rest";
	private static final int WEBUI_MEMORY_IN_MB = 512;
	private static final int WEBUI_PORT = 8099;
	private static final String WEBUI_FILE = "tools" + File.separator + "gs-webui" + File.separator + "gs-webui.war";
	private static final String WEBUI_NAME = "webui";
	private static final String MANAGEMENT_SPACE_NAME = CloudifyConstants.MANAGEMENT_SPACE_NAME;

	private static final String LINUX_SCRIPT_PREFIX = "#!/bin/bash\n";
	private static final String MANAGEMENT_GSA_ZONE = "management";
	private static final long WAIT_EXISTING_AGENT_TIMEOUT_SECONDS = 10;
	
	// management agent starts 1 global esm, 1 gsm,1 lus
	String[] CLOUD_MANAGEMENT_ARGUMENTS = new String[] { 
		"gsa.global.lus","0",
		"gsa.lus","1",
		"gsa.gsc","0",
		"gsa.global.gsm", "0",
		"gsa.gsm" , "1",
		"gsa.global.esm", "1"
	};
	
	// localcloud management agent starts 1 esm, 1 gsm,1 lus
	String[] LOCALCLOUD_MANAGEMENT_ARGUMENTS = new String[] { 
		"gsa.global.lus","0",
		"gsa.lus","0",
		"gsa.gsc","0",
		"gsa.global.gsm", "0",
		"gsa.gsm_lus" , "1",
		"gsa.global.esm", "0",
		"gsa.esm", "1"
	};
		
	
	String[] AGENT_ARGUMENTS = new String[] {
		"gsa.global.lus", "0",
		"gsa.gsc","0",
		"gsa.global.gsm", "0",
		"gsa.global.esm", "0"	
	};

	// script must spawn a daemon process (that is not a child process)
	String[] WINDOWS_COMMAND = new String[] { "cmd.exe", "/c" , "gs-agent.bat" };
	String[] LINUX_COMMAND = new String[] {"gs-agent.sh" };

	// script must suppress output, since this process is not consuming it and
	// so any output could block it.
	String[] WINDOWS_ARGUMENTS_POSTFIX = new String[] { 
			">nul", "2>&1"
	};
	
	String[] LINUX_ARGUMENTS_POSTFIX = new String[] {
			">/dev/null", "2>&1"
	};

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private boolean verbose;
	private String lookupGroups;
	private String lookupLocators;
	private String nicAddress;
	private String zone;
	private int progressInSeconds;
	private AdminFacade adminFacade;
	private boolean noWebServices;
	private boolean noManagementSpace;
	private boolean notHighlyAvailableManagementSpace;
	private int lusPort = DEFAULT_LUS_PORT;
	private boolean autoShutdown;
	private boolean waitForWebUi;
	private String cloudContents;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setLookupGroups(String lookupGroups) {
		this.lookupGroups = lookupGroups;
	}

	public void setLookupLocators(String lookupLocators) {
		this.lookupLocators = lookupLocators;
	}

	public void setNicAddress(String nicAddress) {
		this.nicAddress = nicAddress;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public void setProgressInSeconds(int progressInSeconds) {
		this.progressInSeconds = progressInSeconds;
	}
	
	public void setAdminFacade(AdminFacade adminFacade) {
		this.adminFacade = adminFacade;
	}
	
    public void setNoWebServices(boolean noWebServices) {
        this.noWebServices = noWebServices;
    }
    
    public void setNoManagementSpace(boolean noManagementSpace) {
		this.noManagementSpace = noManagementSpace;
	}

	public void setAutoShutdown(boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}
	
	public void setWaitForWebui(boolean waitForWebui) {
	    this.waitForWebUi = waitForWebui;
	}
	
    public void setNotHighlyAvailableManagementSpace(
            boolean notHighlyAvailableManagementSpace) {
        this.notHighlyAvailableManagementSpace = notHighlyAvailableManagementSpace;
    }

    public boolean isNotHighlyAvailableManagementSpace() {
        return notHighlyAvailableManagementSpace;
    }
	
	public void startLocalCloudOnLocalhostAndWait(int timeout, TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		setDefaultNicAddress();
		
		setDefaultLocalcloudLookup();
		
		startManagementOnLocalhostAndWaitInternal(LOCALCLOUD_MANAGEMENT_ARGUMENTS, timeout, timeunit, true);
	}

	private void setDefaultNicAddress() throws CLIException {
		
		if (nicAddress == null) {
			try {
				nicAddress = Constants.getHostAddress();
			} catch (UnknownHostException e) {
				throw new CLIException(e);
			}
		}
		
		if (verbose) {
			logger.info("NIC Address=" + nicAddress);
		}
	}

	private static String getLocalcloudLookupGroups() {
		return LOCALCLOUD_LOOKUPGROUP;
	}
	
	private String getLocalcloudLookupLocators() {
		if (nicAddress == null) {
			throw new IllegalStateException("nicAddress cannot be null");
		}
		return nicAddress +":" + lusPort;
	}
	
	public void startManagementOnLocalhostAndWait(int timeout,TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		setZone(MANAGEMENT_GSA_ZONE);
		
		setDefaultNicAddress();
		
		startManagementOnLocalhostAndWaitInternal(CLOUD_MANAGEMENT_ARGUMENTS, timeout, timeunit, false);
	}
	
	public void shutdownAgentOnLocalhostAndWait(boolean force, int timeout,TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		setDefaultNicAddress();
		
		shutdownAgentOnLocalhostAndWaitInternal(false, force, timeout,timeunit);
	}
	
	public void shutdownManagementOnLocalhostAndWait(int timeout,TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		setDefaultNicAddress();
		
		shutdownAgentOnLocalhostAndWaitInternal(true, true, timeout,timeunit);
	}

	public void teardownLocalCloudOnLocalhostAndWait(
			long timeout, TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
		
		setDefaultNicAddress();
		
		setDefaultLocalcloudLookup();
		
		shutdownAgentOnLocalhostAndWaitInternal(true, true, timeout, timeunit);
	}

	private void setDefaultLocalcloudLookup() {
		
		if (zone != null) {
			throw new IllegalStateException("Local-cloud does not use zones");
		}
		
		lusPort = LOCALCLOUD_LUS_PORT;
		
		if (lookupLocators == null) {
			setLookupLocators(getLocalcloudLookupLocators());
		}
		
		if (lookupGroups == null) {
			setLookupGroups(getLocalcloudLookupGroups());
		}
	}
	
	public void shutdownAgentOnLocalhostAndWaitInternal(
			boolean allowManagement,
			boolean allowContainers,
			long timeout, 
			TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter(); 
		connectionLogs.supressConnectionErrors();
		adminFacade.disconnect();
		final Admin admin = createAdmin();
		GridServiceAgent agent = null;
		try {
			setLookupDefaults(admin);
			try {
				agent = waitForExistingAgent(admin, WAIT_EXISTING_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
			catch (TimeoutException e) {
			}
			
			if (agent == null) {
				logger.info("Agent not running on local machine");
			}
			else {
				if (!allowContainers) {
					for (ProcessingUnit pu : admin.getProcessingUnits()) {
						for (ProcessingUnitInstance instance : pu) {
							if (agent.equals(instance.getGridServiceContainer().getGridServiceAgent())) {
								throw new CLIException("Cannot shutdown agent since " + pu.getName() + " service is still running on this machine. Use -force flag.");
							}
						}
					}
				}
				
				if (!allowManagement) {
					String message = "Cannot shutdown agent since management processes running on this machine. Use the shutdown-management command instead.";
					
					for (GridServiceManager gsm : admin.getGridServiceManagers()) {
						if (agent.equals(gsm.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}
					
					for (ElasticServiceManager esm : admin.getElasticServiceManagers()) {
						if (agent.equals(esm.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}
					
					for (LookupService lus : admin.getLookupServices()) {
						if (agent.equals(lus.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}
				}
				//Close admin before shutting down the agent to avoid false warning messages the admin will create
				//if it concurrently monitor things that are shutting down.
				admin.close();
				shutdownAgentAndWait(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE,end),TimeUnit.MILLISECONDS);
			}
		}
		finally {
			//close in case of exception, admin support double close if already closed
			admin.close();
			if (agent != null) {
				// admin.close() command does not verify that all of the internal lookup threads are actually terminated
				// therefore we need to suppress connection warnings a little while longer 
				Thread.sleep(WAIT_AFTER_ADMIN_CLOSED_MILLIS);
			}
			connectionLogs.restoreConnectionErrors();
		}
	}

	private void shutdownAgentAndWait(
			final GridServiceAgent agent, 
			long timeout, TimeUnit timeunit) 
		throws InterruptedException, TimeoutException, CLIException {
		
		//We need to shutdown the agent after we close the admin to avoid closed exception since the admin still monitors
		//the deployment behind the scenes, we call the direct proxy to the gsa since the admin is closed and we don't
		//want to use objects it generated
		final GSA gsa = ((InternalGridServiceAgent)agent).getGSA();		
		try {
			gsa.shutdown();
		} catch (RemoteException e) {
			if (!NetworkExceptionHelper.isConnectOrCloseException(e)) {
                throw new AdminException("Failed to shutdown GSA", e);
            }
		}
		
		createConditionLatch(timeout, timeunit)
		.waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException,
					InterruptedException {
				logger.info("Waiting for agent to shutdown");
				
				try {
					gsa.ping();
				} catch (RemoteException e) {
					//Probably NoSuchObjectException meaning the GSA is going down
					return true;
				}
				
				return false;
			}
			
		});
	}

	private void runGsAgentOnLocalHost(String name, String[] gsAgentArguments) throws CLIException, InterruptedException {
		
		List<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(gsAgentArguments));
		
		String[] command;
		if (isWindows()) {
			command = WINDOWS_COMMAND;
			args.addAll(Arrays.asList(WINDOWS_ARGUMENTS_POSTFIX));
		}
		else {
			command = LINUX_COMMAND;
			args.addAll(Arrays.asList(LINUX_ARGUMENTS_POSTFIX));
		}
		
		logger.info("Starting " + name + (verbose ? ":\n" + 
		        StringUtils.collectionToDelimitedString(Arrays.asList(command), " ") + " " + 
		        StringUtils.collectionToDelimitedString(args, " "): ""));
		runCommand(command, args.toArray(new String[args.size()]));
		
	}

	private void startManagementOnLocalhostAndWaitInternal(String[] gsAgentArgs, int timeout,TimeUnit timeunit, boolean isLocalCloud) 
		throws CLIException, InterruptedException, TimeoutException {
		long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		
		ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter(); 
		connectionLogs.supressConnectionErrors();
		final Admin admin = createAdmin();
		try {
			setLookupDefaults(admin);
			GridServiceAgent agent;
			try {
				try {
					if (!isLocalCloud || fastExistingAgentCheck()){						
						waitForExistingAgent(admin, progressInSeconds, TimeUnit.SECONDS);
						throw new CLIException("Agent already running on local machine.");
					}
				}
				catch (TimeoutException e ) {
					// no existing agent running on local machine
				}
			
				runGsAgentOnLocalHost("agent and management processes", gsAgentArgs);
				agent = waitForNewAgent(admin, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
			} finally {
				connectionLogs.restoreConnectionErrors();				
			}
			waitForManagementProcesses(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
			
			List<AbstractManagementServiceInstaller> waitForManagementServices = new LinkedList<AbstractManagementServiceInstaller>();
			
			connectionLogs.supressConnectionErrors();
			try {
				ManagementSpaceServiceInstaller managementSpaceInstaller = null;
				if (!noManagementSpace) {
					final boolean highlyAvailable = !isLocalCloud && !notHighlyAvailableManagementSpace;
					managementSpaceInstaller = new ManagementSpaceServiceInstaller();
					managementSpaceInstaller.setAdmin(agent.getAdmin());
					managementSpaceInstaller.setVerbose(verbose);
					managementSpaceInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
					managementSpaceInstaller.setMemory(MANAGEMENT_SPACE_MEMORY_IN_MB, MemoryUnit.MEGABYTES);
					managementSpaceInstaller.setServiceName(MANAGEMENT_SPACE_NAME);
					managementSpaceInstaller.setManagementZone(MANAGEMENT_GSA_ZONE);
					managementSpaceInstaller.setHighlyAvailable(highlyAvailable);
    				try {
    					managementSpaceInstaller.install();
    					waitForManagementServices.add(managementSpaceInstaller);
    				}
    				catch (ProcessingUnitAlreadyDeployedException e) {
    					if (verbose) {
    						logger.info("Service " + MANAGEMENT_SPACE_NAME + " already installed");
    					}
    				}
				}
				
				if (!noWebServices) {
    				ManagementWebServiceInstaller webuiInstaller = new ManagementWebServiceInstaller();    				
    				webuiInstaller.setAdmin(agent.getAdmin());
    				webuiInstaller.setVerbose(verbose);
    				webuiInstaller.setProgress(progressInSeconds,TimeUnit.SECONDS);
    				webuiInstaller.setMemory(WEBUI_MEMORY_IN_MB, MemoryUnit.MEGABYTES);
    				webuiInstaller.setPort(WEBUI_PORT);
    				webuiInstaller.setWarFile(new File(WEBUI_FILE));
    				webuiInstaller.setServiceName(WEBUI_NAME);
    				webuiInstaller.setManagementZone(MANAGEMENT_GSA_ZONE);
    				try {
    					webuiInstaller.install();    					
    				}
    				catch (ProcessingUnitAlreadyDeployedException e) {
    					if (verbose) {
    						logger.info("Service " + WEBUI_NAME + " already installed");
    					}
    				}
    				if (waitForWebUi)
        				waitForManagementServices.add(webuiInstaller);
    				else
    					webuiInstaller.logServiceLocation();
    				
    				ManagementWebServiceInstaller restInstaller = new ManagementWebServiceInstaller();
    				restInstaller.setAdmin(agent.getAdmin());
    				restInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
    				restInstaller.setVerbose(verbose);
    				
    				restInstaller.setMemory(REST_MEMORY_IN_MB, MemoryUnit.MEGABYTES);
    				restInstaller.setPort(REST_PORT);
    				restInstaller.setWarFile(new File(REST_FILE));
    				restInstaller.setServiceName(REST_NAME);
    				restInstaller.setManagementZone(MANAGEMENT_GSA_ZONE);
    				restInstaller.setWaitForConnection();
    				try {
    					restInstaller.install();
    				}
    				catch (ProcessingUnitAlreadyDeployedException e) {
    					if (verbose) {
    						logger.info("Service " + REST_NAME + " already installed");
    					}
    				}
    				waitForManagementServices.add(restInstaller);
    				
    			} 
				
				
				
				
				for (AbstractManagementServiceInstaller managementServiceInstaller : waitForManagementServices) {
					managementServiceInstaller.waitForInstallation(adminFacade, agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
					if(managementServiceInstaller instanceof ManagementSpaceServiceInstaller) {
						logger.info("Writing cloud configuration to space.");
						GigaSpace gigaspace = managementSpaceInstaller.getGigaSpace();
						
						CloudConfigurationHolder holder = new CloudConfigurationHolder(getCloudContents());
						gigaspace.write(holder);
						// Shut down the space proxy so that if the cloud is torn down later, there will not be any discovery errors.
						// Note: in a spring environment, the bean shutdown would clean this up.
						// TODO - this is a hack. Move the space writing part into the management space installer and do the clean up there.
						((ISpaceProxy) gigaspace.getSpace()).close();
					}
				}
				

				
			} finally {
				connectionLogs.restoreConnectionErrors();
			}			
		}		
		finally {
			admin.close();
		}
	}
	
	private boolean fastExistingAgentCheck() {
		return !ServiceUtils.isPortFree(lusPort);
	}

	/**
	 * This method assumes that the admin has been supplied with this.lookupLocators and this.lookupGroups
	 * and that it applied the defaults if these were null.
	 * @param admin
	 */
	private void setLookupDefaults(Admin admin) {
		if (admin.getGroups().length == 0 || admin.getGroups() == null) {
			throw new IllegalStateException("Admin lookup group must be set");
		}
		this.lookupGroups = StringUtils.arrayToDelimitedString(admin.getGroups(),",");
		final LookupLocator[] locators = admin.getLocators();
		if (locators != null && locators.length > 0) {
			this.lookupLocators = convertLookupLocatorToString(locators);
		}
	}

	public static String convertLookupLocatorToString(LookupLocator[] locators) {
		final List<String> trimmedLocators = new ArrayList<String>();
		if (locators != null) {
			for (final LookupLocator locator : locators) {
				trimmedLocators.add(locator.getHost() + ":" + locator.getPort());
			}
		}
		return StringUtils.collectionToDelimitedString(trimmedLocators,",");
	}
	
	public void startAgentOnLocalhostAndWait(long timeout, TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {
		
		if (zone == null || zone.length() == 0) {
			throw new CLIException("Agent must be started with a zone");
		}
		
		setDefaultNicAddress();
		
		ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter(); 
		connectionLogs.supressConnectionErrors();
		final Admin admin = createAdmin();
		try {
			setLookupDefaults(admin);
					
			try {
				waitForExistingAgent(admin, WAIT_EXISTING_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				throw new CLIException("Agent already running on local machine. Use shutdown-agent first.");
			}
			catch (TimeoutException e ) {
				// no existing agent running on local machine
			}
			runGsAgentOnLocalHost("agent", AGENT_ARGUMENTS);
			
			// wait for agent to start
			waitForNewAgent(admin, timeout, timeunit);
		}
		finally {
			admin.close();
			connectionLogs.restoreConnectionErrors();
		}
	}

	private void waitForManagementProcesses(final GridServiceAgent agent, long timeout, TimeUnit timeunit)
			throws TimeoutException, InterruptedException, CLIException {

		final Admin admin = agent.getAdmin();
		
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			
				@Override
				public boolean isDone() throws CLIException,
						InterruptedException {

					boolean isDone = true;
					
					if (!isDone(admin.getLookupServices(),"LUS")) {
						if (verbose) {
							logger.info("Waiting for Lookup Service");
						}
						isDone = false;
					}
					
					if (!isDone(admin.getGridServiceManagers(),"GSM")) {
						if (verbose) {
							logger.info("Waiting for Grid Service Manager");
						}
						isDone = false;
					}
					
					if (admin.getElasticServiceManagers().isEmpty()) {
						if (verbose) {
							logger.info("Waiting for Elastic Service Manager");
						}
						isDone = false;
					}
					
					if (!verbose) {
						logger.info("Waiting for Management processes to start.");
					}
					
					return isDone;
				}

				@SuppressWarnings("rawtypes")
				private boolean isDone(Iterable<? extends AgentGridComponent> components, String serviceName) {

					boolean found = false;
					for (AgentGridComponent component : components) {
						if (checkAgent(component)) {
							found = true;
							break;
						}						
					}
					
					if (verbose) {
						for (Object component : components) {
							GridServiceAgent agentThatStartedComponent = ((AgentGridComponent)component).getGridServiceAgent();
							String agentUid = null;
							if (agentThatStartedComponent != null) {
								agentUid = agentThatStartedComponent.getUid();
							}
							String message = 
									"Detected " + serviceName + " management process " + 
									" started by agent " + agentUid + " ";
							if (!checkAgent((AgentGridComponent)component)) {
								message +=  " expected agent " + agent.getUid();
							}
							logger.info(message);	
						}
					}
					
					return found;
				}

				private boolean checkAgent(AgentGridComponent component) {
					return agent.equals(component.getGridServiceAgent());
				}
			
			});
	}

	private GridServiceAgent waitForExistingAgent(final Admin admin, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		return waitForAgent(admin, true, timeout, timeunit);
	}
	
	private GridServiceAgent waitForNewAgent(final Admin admin, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		return waitForAgent(admin, false, timeout, timeunit);
	}
	
	private GridServiceAgent waitForAgent(final Admin admin, final boolean existingAgent, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		
		final AtomicReference<GridServiceAgent> agentOnLocalhost = new AtomicReference<GridServiceAgent>();
		
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
		
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				
				boolean isDone = false;
				for (GridServiceAgent agent : admin.getGridServiceAgents()) {
					if (checkAgent(agent)) {
						agentOnLocalhost.set(agent);
						isDone = true;
						break;
					}
				}
				if (!isDone) {
					if (existingAgent) {
						logger.info("Looking for an existing agent running on local machine");
					}
					else {
						logger.info("Waiting for the agent on the local machine to start.");
					}
				}
				return isDone;
			}

			private boolean checkAgent(GridServiceAgent agent) {
				final String agentNicAddress = agent.getMachine().getHostAddress();
				final String agentLookupGroups = getLookupGroups(agent);
				boolean checkLookupGroups = lookupGroups != null && lookupGroups.equals(agentLookupGroups);
				boolean checkNicAddress = (nicAddress != null && agentNicAddress.equals(nicAddress)) || isThisMyIpAddress(agentNicAddress);
				if (verbose) {
					String message = "Discovered agent nic-address=" + agentNicAddress + " lookup-groups=" + agentLookupGroups + ". ";
					if (!checkLookupGroups) {
						message += "Ignoring agent. Filter lookupGroups='" + lookupGroups + "', agent LookupGroups='" + agentLookupGroups +"'";
					}
					if (!checkNicAddress) {
						message += "Ignoring agent. Filter nicAddress='" + nicAddress + "' or local address, agent nicAddress='" + agentNicAddress +"'";
					}
					logger.info(message);
				}
				return checkLookupGroups &&	checkNicAddress;
			}
			
			/**
			 * @see http://stackoverflow.com/questions/2406341/how-to-check-if-an-ip-address-is-the-local-host-on-a-multi-homed-system
			 */
			public boolean isThisMyIpAddress(String ip) {
				InetAddress addr;
				try {
					addr = InetAddress.getByName(ip);
				} catch (UnknownHostException e) {
					return false;
				}
				
				// Check if the address is a valid special local or loop back
			    if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
			        return true;

			    // Check if the address is defined on any interface
			    try {
			        return NetworkInterface.getByInetAddress(addr) != null;
			    } catch (SocketException e) {
			        return false;
			    }
			}


		    private String getLookupGroups(VirtualMachineAware component) {
		        
		        String prefix = "-Dcom.gs.jini_lus.groups=";
		        return getCommandLineArgumentRemovePrefix(component,prefix);
		    }
		    
			private String getCommandLineArgumentRemovePrefix(VirtualMachineAware component, String prefix) {
		        String[] commandLineArguments = component.getVirtualMachine().getDetails().getInputArguments();
		        String requiredArg = null;
		        for (final String arg : commandLineArguments) {
		            if (arg.startsWith(prefix)) {
		                requiredArg = arg;
		            }
		        }
		        
		        if (requiredArg != null) {
		            return requiredArg.substring(prefix.length());
		        }
		        return null;
		    }
		});
		
		return agentOnLocalhost.get();							
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private void runCommand(String[] command, String[] args) throws CLIException,
			InterruptedException {

		File directory = new File(Environment.getHomeDirectory(), "/bin").getAbsoluteFile();
		
		// gs-agent.sh/bat need full path
		command[command.length-1] = new File(directory, command[command.length-1]).getAbsolutePath();
		
		List<String> commandLine = new ArrayList<String>();
		commandLine.addAll(Arrays.asList(command));
		commandLine.addAll(Arrays.asList(args));
				
		File filename = createScript(StringUtils.collectionToDelimitedString(commandLine, " "));
		final ProcessBuilder pb = 
			new ProcessBuilder()
			.command(filename.getAbsolutePath())
			.directory(directory);

		String gsaJavaOptions = "-Xmx" + GSA_MEMORY_IN_MB + "m";
		if (autoShutdown) {
			gsaJavaOptions += " " + AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT;
		}
		String lusJavaOptions = 
				"-Xmx" + LUS_MEMORY_IN_MB + "m"
			  + " -D" + LUS_PORT_CONTEXT_PROPERTY +"=" + lusPort;
		String gsmJavaOptions = "-Xmx" + GSM_MEMORY_IN_MB + "m"
			  + " -D" + LUS_PORT_CONTEXT_PROPERTY +"=" + lusPort;
		String esmJavaOptions = "-Xmx" + ESM_MEMORY_IN_MB + "m";
		String gscJavaOptions = "";
		
		if (lookupGroups != null) {
			pb.environment().put("LOOKUPGROUPS", lookupGroups);
		}

		if (lookupLocators != null) {
			pb.environment().put("LOOKUPLOCATORS", lookupLocators);
			String disableMulticast = "-Dcom.gs.multicast.enabled=false";	
			gsaJavaOptions += " " + disableMulticast;
			lusJavaOptions += " " + disableMulticast;
			gsmJavaOptions += " " + disableMulticast;
			esmJavaOptions += " " + disableMulticast;
			gscJavaOptions += disableMulticast;
		}

		if (nicAddress != null) {
			pb.environment().put("NIC_ADDR", nicAddress);
		}

		if (zone != null) {
			gsaJavaOptions += " -Dcom.gs.zones=" + zone;
		}

		pb.environment().put("GSA_JAVA_OPTIONS", gsaJavaOptions);
		pb.environment().put("LUS_JAVA_OPTIONS", lusJavaOptions);
		pb.environment().put("GSM_JAVA_OPTIONS", gsmJavaOptions);
		pb.environment().put("ESM_JAVA_OPTIONS", esmJavaOptions);
		pb.environment().put("GSC_JAVA_OPTIONS", gscJavaOptions);

		// start process 
		// there is no need to redirect output, since the process suppresses
		// output
		try {
			pb.start();
		} catch (final IOException e) {
			throw new CLIException("Error while starting agent", e);
		}
	}

	private Admin createAdmin() {
		final AdminFactory adminFactory = new AdminFactory();
		if (lookupGroups != null) {
			adminFactory.addGroups(lookupGroups);
		}
		
		if (lookupLocators != null) {
			adminFactory.addLocators(lookupLocators);
		}
		

		final Admin admin = adminFactory.create();
		if (verbose) {
			if (admin.getLocators().length > 0) {
				logger.info("Lookup Locators=" + convertLookupLocatorToString(admin.getLocators()));
			}
			
			if (admin.getGroups().length > 0) {
				logger.info("Lookup Groups=" + StringUtils.arrayToDelimitedString(admin.getGroups(),","));
			}
		}
		return admin;
	}
	
	private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
			new ConditionLatch()
			.timeout(timeout,timeunit)
			.pollingInterval(progressInSeconds, TimeUnit.SECONDS)
			.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
			.verbose(verbose);
	}
	
    private File createScript(String text) throws CLIException {
        File tempFile;
        try {
            tempFile = File.createTempFile("run-gs-agent", isWindows() ? ".bat"
                    : ".sh");
        } catch (IOException e) {
            throw new CLIException(e);
        }
        tempFile.deleteOnExit();
        BufferedWriter out = null;
        try {
            try {
                out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(tempFile)));
                if (!isWindows()) {
                    out.write(LINUX_SCRIPT_PREFIX);
                }
                out.write(text);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (final IOException e) {
            throw new CLIException(e);
        }
        if (!isWindows()) {
            tempFile.setExecutable(true, true);
        }
        return tempFile;
    }

	public String getCloudContents() {
		return cloudContents;
	}

	public void setCloudContents(String cloudContents) {
		this.cloudContents = cloudContents;
	}

}
