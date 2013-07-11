/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * resolver for a prepared, packed application file.
 *   
 * @author adaml
 *
 */
public class PreparedApplicationPackageResolver implements NameAndPackedFileResolver {

	private Application application;
	private File packedFile;
	
	private boolean initialized = false;
	private File overridesFile;
	
	public PreparedApplicationPackageResolver(final File packedFile, 
												final File overridesFile) {
		this.packedFile = packedFile;
		this.overridesFile = overridesFile;
	}
	
	@Override
	public String getName() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return application.getName();
	}

	@Override
	public File getPackedFile() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return packedFile;
	}

    @Override
    public Map<String, Integer> getPlannedNumberOfInstancesPerService() 
    		throws CLIStatusException {
    	if (!initialized) {
    		init();
    	}
    	final Map<String, Integer> deploymentIDsMap = new HashMap<String, Integer>(); 
        for (Service service : application.getServices()) {
			deploymentIDsMap.put(service.getName(), service.getNumInstances());
		}
        return deploymentIDsMap;
    }
    
    private void init() throws CLIStatusException {
    	File applicationFile = null;
    	try {
            final File applicationFolder = ServiceReader.extractProjectFile(packedFile);
            applicationFile = DSLReader
            				.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationFolder);
            final DSLApplicationCompilatioResult result = ServiceReader.getApplicationFromFile(applicationFile, 
            																		this.overridesFile);
            this.application = result.getApplication();
    	} catch (final IOException e) {
    		throw new CLIStatusException("failed_while_reading_dsl_file",
    									 e.getMessage(), e);
    	} catch (final DSLException e) {
    		throw new CLIStatusException("failed_while_reading_dsl_file",
    									 e.getMessage(), e);
    	}
    	
	}

	@Override
	public Object getDSLObject() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return this.application;
	}

}
