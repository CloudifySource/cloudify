package org.cloudifysource.usm.dsl;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.usm.CommandParts;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;




public class DSLConfiguration implements UniversalServiceManagerConfiguration {

	private final Service service;
	private final File puExtDir;
	private final ServiceContext serviceContext;
	private final File serviceFile;

	public File getServiceFile() {
		return serviceFile;
	}

	public ServiceContext getServiceContext() {
		return serviceContext;
	}

	public DSLConfiguration(final Service service, ServiceContext serviceContext, final File puExtDir, final File serviceFile) {
		this.service = service;
		this.serviceContext = serviceContext;
		this.puExtDir = puExtDir;
		this.serviceFile = serviceFile;
	}
	
	@Override
	public Object getStartCommand() {
		final Object start = this.service.getLifecycle().getStart();

		return start;

	}

	@Override
	public int getNumberOfLaunchRetries() {
		return 0;
	}

	@Override
	public String getPidFile() {
		return null;
	}

	public CommandParts getWindowsCommandParts() {
		return null;
	}

	public CommandParts getLinuxCommandParts() {
		return null;
	}

	public Service getService() {
		return service;
	}

	public File getPuExtDir() {
		return puExtDir;
	}

	@Override
	public String getServiceName() {
		return this.service.getName();
	}

	@Override
	public long getStartDetectionTimeoutMSecs() {
		return this.service.getLifecycle().getStartDetectionTimeoutSecs() * 1000;
	}

	@Override
	public long getStartDetectionIntervalMSecs() {
		return this.service.getLifecycle().getStartDetectionIntervalSecs() * 1000;
	}

}
