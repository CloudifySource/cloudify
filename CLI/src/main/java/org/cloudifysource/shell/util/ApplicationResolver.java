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
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * Resolver for an application folder.
 * 
 * @author adaml
 *
 */
public class ApplicationResolver implements NameAndPackedFileResolver {

	private File applicationDir;
	private File overridesFile;
	private Application application;
	private boolean initialized = false;
	
	protected static final Logger logger = Logger.getLogger(ApplicationResolver.class.getName());
	
	public ApplicationResolver(final File appDir, final File overridesFile) {
		this.applicationDir = appDir;
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
		try {
			if (!initialized) {
				init();
			}
			return Packager.packApplication(application, applicationDir);
		} catch (final IOException e) {
			throw new CLIStatusException(e, "failed_to_package_application",
					applicationDir);
		} catch (final PackagingException e) {
			throw new CLIStatusException(e, "failed_to_package_application",
					applicationDir);
		}
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
		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveApplication(applicationDir)) {
			applicationDir = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("application_not_found",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}
		final File dslFile = DSLReader.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationDir);
		final DSLReader dslReader = createDslReader(dslFile);
		try {
			this.application = dslReader.readDslEntity(Application.class);
		} catch (DSLException e) {
			throw new CLIStatusException(e, "failed_while_reading_dsl_file",
								e.getMessage());
		}
		initialized = true;
	}

	private DSLReader createDslReader(final File dslFile) {
		final DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());
		dslReader.setOverridesFile(this.overridesFile);
		return dslReader;
	}

	@Override
	public Object getDSLObject() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return this.application;
	}
}
