package com.gigaspaces.cloudify.shell.installer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.ServiceDetails;

import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.ConditionLatch;
import com.gigaspaces.cloudify.shell.commands.CLIException;
import com.gigaspaces.cloudify.shell.rest.ErrorStatusException;
import com.j_spaces.kernel.Environment;

public class ManagementWebServiceInstaller extends AbstractManagementServiceInstaller {
	
	private int port;
	private File warFile;
	private boolean waitForConnection;
	
	public void setProgress(int progress, TimeUnit timeunit) {
		this.progressInSeconds = timeunit.toSeconds(progress);
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public void setWarFile(File warFile) {
		this.warFile = warFile;
	}
	
	@Override
	public void install() throws CLIException, ProcessingUnitAlreadyDeployedException {
		
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
	
	@Override
	public void waitForInstallation(AdminFacade adminFacade, GridServiceAgent agent, long timeout,
			TimeUnit timeunit) throws ErrorStatusException, InterruptedException, TimeoutException, CLIException {
		long startTime = System.currentTimeMillis();
		URL url = waitForProcessingUnitInstance(agent, timeout, timeunit);
		long remainingTime = timeunit.toMillis(timeout) - (System.currentTimeMillis() - startTime);
		if (waitForConnection)
			waitForConnection(adminFacade, url, remainingTime, TimeUnit.MILLISECONDS);
	}
	
	public void setWaitForConnection() {
		waitForConnection = true;
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
	
	private void waitForConnection(final AdminFacade adminFacade, final URL url, final long timeout, final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException { 
		adminFacade.disconnect();
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
			
	            try {
	                adminFacade.connect(null, null, url.toString());
	                return true;
	            } catch (ErrorStatusException e) {
	                if (verbose) {
	                	logger.log(Level.INFO,"Error connecting to web service [" + serviceName + "].",e);
	                }
	            }
	            logger.log(Level.INFO,"Connecting to web service [" + serviceName + "].");
	            return false;
			}
		});
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
    
    @Override
	protected Properties getContextProperties() {
    	Properties props = super.getContextProperties();
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
