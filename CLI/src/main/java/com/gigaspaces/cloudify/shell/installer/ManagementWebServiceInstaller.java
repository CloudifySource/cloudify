package com.gigaspaces.cloudify.shell.installer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;

import com.gigaspaces.cloudify.shell.ConditionLatch;
import com.gigaspaces.cloudify.shell.commands.CLIException;
import com.gigaspaces.cloudify.shell.rest.ErrorStatusException;
import com.j_spaces.kernel.Environment;

public class ManagementWebServiceInstaller {
	
	private static final String TIMEOUT_ERROR_MESSAGE = "operation timed out waiting for the rest service to start";
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private Admin admin;
	private boolean verbose;
	private long progressInSeconds;
	private int memoryInMB;
	private int port;
	private File warFile;
	private String serviceName;
	private String zone;
	
	private static final int RESERVED_MEMORY_IN_MB = 256;
	public static final String MANAGEMENT_APPLICATION_NAME = "management";
	
	public void setProgress(int progress, TimeUnit timeunit) {
		this.progressInSeconds = timeunit.toSeconds(progress);
	}
	
	public void setAdmin(Admin admin) {
		this.admin = admin;
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setMemory(long memory, MemoryUnit unit) {
		this.memoryInMB = (int) unit.toMegaBytes(memory);
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public void setWarFile(File warFile) {
		this.warFile = warFile;
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setManagementZone(String zone) {
		this.zone = zone;
	}
	
	public void install() throws TimeoutException, InterruptedException, CLIException, ProcessingUnitAlreadyDeployedException {
		
		if (zone == null) {
			throw new IllegalStateException("Management services must be installed on management zone");
		}
		
		final ElasticStatelessProcessingUnitDeployment deployment = 
			new ElasticStatelessProcessingUnitDeployment(getGSFile(warFile))
			.memoryCapacityPerContainer(memoryInMB, MemoryUnit.MEGABYTES)
			.name(serviceName)
			// All PUs on this role share the same machine. Machines
			// are identified by zone.
			.sharedMachineProvisioning(
					"public",
					new DiscoveredMachineProvisioningConfigurer()
							.addGridServiceAgentZone(zone)
							.reservedMemoryCapacityPerMachine(RESERVED_MEMORY_IN_MB, MemoryUnit.MEGABYTES)
							.create())
			// Eager scale (1 container per machine per PU)
			.scale(new EagerScaleConfigurer()
			       .atMostOneContainerPerMachine()
				   .create());

		for (Entry<Object, Object> prop : getContextProperties().entrySet()) {
			deployment.addContextProperty(prop.getKey().toString(),prop.getValue().toString());
		}
		getGridServiceManager().deploy(deployment);
	}
	
	private GridServiceManager getGridServiceManager() throws CLIException {
		Iterator<GridServiceManager> it = admin.getGridServiceManagers().iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw new CLIException("No Grid Service Manager found to deploy " + serviceName);
   }

	public URL waitForProcessingUnitInstance(
			final GridServiceAgent agent,
			final long timeout, 
			final TimeUnit timeunit)
	
			throws InterruptedException, TimeoutException, CLIException,ErrorStatusException {
		
		createConditionLatch(timeout,timeunit).waitFor( new ConditionLatch.Predicate() {
			
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				logger.info("Waiting for " + serviceName + " service.");
				ProcessingUnit pu = getProcessingUnit();
				boolean isDone = false;
				if (pu != null) {
					for (ProcessingUnitInstance instance : pu) {
						if (agent.equals(instance.getGridServiceContainer().getGridServiceAgent())) {
							isDone = true;
							break;
						}
					}
				}
				return isDone;
			}
		});
		
        URL url = getWebProcessingUnitURL(agent, getProcessingUnit());
        String serviceNameCapital = StringUtils.capitalize(serviceName);
        logger.info(serviceNameCapital + " service is available at: " + url);
        return url;
	}
	
    public void logServiceLocation() throws CLIException {
        try {
            String serviceNameCapital = StringUtils.capitalize(serviceName);
            String localhost = Constants.getHostAddress();
            logger.info(serviceNameCapital + " service will be available at: http://" + localhost + ":" + port);
        } catch (UnknownHostException e) {
            throw new CLIException("Failed getting host address", e);
        }
    }
    
	private Properties getContextProperties() {
		Properties props = new Properties();
		props.put("com.gs.application", MANAGEMENT_APPLICATION_NAME);
		props.put("web.port", String.valueOf(port));
		props.put("web.context", "/");
		props.put("web.context.unique", "true");
		return props;
	}

	public void waitForManagers(final long timeout, final TimeUnit timeunit) throws ErrorStatusException, InterruptedException, TimeoutException, CLIException {

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			
			@Override
			public boolean isDone() throws CLIException, InterruptedException,
					ErrorStatusException {

				boolean isDone = true;
				if (0 == admin.getGridServiceManagers().getSize()) {
					isDone = false;
					if (verbose) {
						logger.info("Waiting for Grid Service Manager");
					}
				}
				
				if (admin.getElasticServiceManagers().getSize() == 0) {
					isDone = false;
					if (verbose) {
						logger.info("Waiting for Elastic Service Manager");
					}
				}
				
				if (!isDone && !verbose) {
					logger.info("Waiting for Cloudify management processes");
				}
				
				return isDone;
			}
		});
		
		admin.getGridServiceManagers().waitForAtLeastOne();
	}
			
	private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
			new ConditionLatch()
			.timeout(timeout,timeunit)
			.pollingInterval(progressInSeconds, TimeUnit.SECONDS)
			.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
			.verbose(verbose);
	}


	private ProcessingUnit getProcessingUnit() {
		return admin.getProcessingUnits().getProcessingUnit(serviceName);
	}

	public static URL getWebProcessingUnitURL(GridServiceAgent agent, ProcessingUnit pu) {
		ProcessingUnitInstance pui = null;
		
		for (ProcessingUnitInstance instance : pu.getInstances()) {
		    if (instance.getGridServiceContainer() != null &&
		        instance.getGridServiceContainer().getGridServiceAgent() != null &&
		        instance.getGridServiceContainer().getGridServiceAgent().equals(agent)) {
		        pui = instance;
		    }
		}
		
		if (pui == null) {
		    throw new IllegalStateException("Failed finding " + pu.getName() + 
		            " on " + agent.getMachine().getHostAddress());
		}
		
		Map<String, ServiceDetails> alldetails = pui
				.getServiceDetailsByServiceId();

		ServiceDetails details = alldetails.get("jee-container");
		String host = details.getAttributes().get("host").toString();
		String port = details.getAttributes().get("port").toString();
		String ctx = details.getAttributes().get("context-path").toString();
		String url = "http://" + host + ":" + port + ctx;
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			// this is a bug since we formed the URL correctly
			throw new IllegalStateException(e);
		}
	}

	public static File getGSFile(File warFile) {
		if (!warFile.isAbsolute()) {
			warFile = new File(Environment.getHomeDirectory(),warFile.getPath());
		}
		return warFile;
	}
	
}
