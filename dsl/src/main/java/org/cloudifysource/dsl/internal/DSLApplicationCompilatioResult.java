package org.cloudifysource.dsl.internal;

import java.io.File;

import org.cloudifysource.dsl.Application;


public class DSLApplicationCompilatioResult {

	private Application application;
	private File applicationDir;
	private File applicationFile;
	
	
	
	
	public DSLApplicationCompilatioResult(Application application,
			File applicationDir, File applicationFile) {
		super();
		this.application = application;
		this.applicationDir = applicationDir;
		this.applicationFile = applicationFile;
	}
	public File getApplicationFile() {
		return applicationFile;
	}
	public void setApplicationFile(File applicationFile) {
		this.applicationFile = applicationFile;
	}
	
	public Application getApplication() {
		return application;
	}
	public void setApplication(Application application) {
		this.application = application;
	}
	public File getApplicationDir() {
		return applicationDir;
	}
	public void setApplicationDir(File applicationDir) {
		this.applicationDir = applicationDir;
	}
	
	
}
