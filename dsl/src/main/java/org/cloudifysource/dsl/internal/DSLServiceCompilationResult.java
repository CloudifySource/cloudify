package org.cloudifysource.dsl.internal;

import java.io.File;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;


public class DSLServiceCompilationResult {
	private Service service;
	private Cloud cloud;
	

	private ServiceContext context;
	private File dslFile;

	public DSLServiceCompilationResult(Service service, ServiceContext context, Cloud cloud,
			File dslFile) {
		super();
		this.service = service;
		this.context = context;
		this.dslFile = dslFile;
		this.cloud = cloud;
	}
	public DSLServiceCompilationResult(Service service, ServiceContext context,
			File dslFile) {
		this(service, context, null, dslFile);
	}

	public Service getService() {
		return service;
	}

	public ServiceContext getContext() {
		return context;
	}

	public File getDslFile() {
		return dslFile;
	}

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(Cloud cloud) {
		this.cloud = cloud;
	}
}