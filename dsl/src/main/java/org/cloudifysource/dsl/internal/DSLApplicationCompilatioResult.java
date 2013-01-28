/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal;

import java.io.File;

import org.cloudifysource.dsl.Application;

/*********
 * Results of loading an Application DSL file.
 * 
 * @author barakme
 * 
 */
public class DSLApplicationCompilatioResult {

	private Application application;
	private File applicationDir;
	private File applicationFile;

	public DSLApplicationCompilatioResult(final Application application,
			final File applicationDir, final File applicationFile) {
		super();
		this.application = application;
		this.applicationDir = applicationDir;
		this.applicationFile = applicationFile;
	}

	public File getApplicationFile() {
		return applicationFile;
	}

	public void setApplicationFile(final File applicationFile) {
		this.applicationFile = applicationFile;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(final Application application) {
		this.application = application;
	}

	public File getApplicationDir() {
		return applicationDir;
	}

	public void setApplicationDir(final File applicationDir) {
		this.applicationDir = applicationDir;
	}

}
