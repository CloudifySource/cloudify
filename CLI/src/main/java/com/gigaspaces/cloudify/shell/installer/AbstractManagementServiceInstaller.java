package com.gigaspaces.cloudify.shell.installer;

import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.core.util.MemoryUnit;

import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.ConditionLatch;
import com.gigaspaces.cloudify.shell.commands.CLIException;
import com.gigaspaces.cloudify.shell.rest.ErrorStatusException;

/**
 * @author eitany
 * @since 2.0
 */
public abstract class AbstractManagementServiceInstaller {

	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	protected Admin admin;
	protected boolean verbose;
	protected int memoryInMB;
	protected String serviceName;
	protected String zone;
	protected long progressInSeconds;
	public static final String MANAGEMENT_APPLICATION_NAME = "management";
	private static final String TIMEOUT_ERROR_MESSAGE = "operation timed out waiting for the rest service to start";
	protected static final int RESERVED_MEMORY_IN_MB = 256;

	public AbstractManagementServiceInstaller() {
		super();
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

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setManagementZone(String zone) {
		this.zone = zone;
	}
	
	public void setProgress(int progress, TimeUnit timeunit) {
		this.progressInSeconds = timeunit.toSeconds(progress);
	}
	
	public abstract void install() throws CLIException, ProcessingUnitAlreadyDeployedException;
	
	public abstract void waitForInstallation(AdminFacade adminFacade, GridServiceAgent agent,
			long timeout, 
			TimeUnit timeunit) throws ErrorStatusException, InterruptedException, TimeoutException, CLIException;
	
	protected GridServiceManager getGridServiceManager() throws CLIException {
		Iterator<GridServiceManager> it = admin.getGridServiceManagers().iterator();
	    if (it.hasNext()) {
	        return it.next();
	    }
	    throw new CLIException("No Grid Service Manager found to deploy " + serviceName);
	}
	
	protected Properties getContextProperties() {
		Properties props = new Properties();
		props.put("com.gs.application", MANAGEMENT_APPLICATION_NAME);
		return props;
	}

	protected ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
			new ConditionLatch()
			.timeout(timeout,timeunit)
			.pollingInterval(progressInSeconds, TimeUnit.SECONDS)
			.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE)
			.verbose(verbose);
	}

}