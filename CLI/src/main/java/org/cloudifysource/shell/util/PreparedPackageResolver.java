/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 11:59 AM
 *
 * Resolves file name and packed file for a prepared package. i.e prepared archive.
 *
 * @since 2.6.0
 */
public class PreparedPackageResolver implements NameAndPackedFileResolver {

    private File zipFile;
    private Service service;

    private boolean initialized = false;

    public PreparedPackageResolver(final File zipFile) {
        this.zipFile = zipFile;
    }

    @Override
    public String getName() throws CLIStatusException {
        if (!initialized) {
            init();
        }
        return service.getName();
    }

    @Override
    public File getPackedFile() throws CLIStatusException {
        if (!initialized) {
            init();
        }
        return zipFile;
    }

    @Override
    public Map<String, Integer> getPlannedNumberOfInstancesPerService() throws CLIStatusException {
        if (!initialized) {
            init();
        }
        final Map<String, Integer> instanceCountMap = new HashMap<String, Integer>();
        instanceCountMap.put(service.getName(), service.getNumInstances());
        //return a map with one entry
        return instanceCountMap;
    }

    private void init() throws CLIStatusException  {
        try {
            this.service = ServiceReader.readServiceFromZip(zipFile);
        } catch (final Exception e) {
            throw new CLIStatusException(e, "read_dsl_file_failed",
                    zipFile, e.getMessage());
        }
    }

	@Override
	public Object getDSLObject() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return this.service;
	}
}
