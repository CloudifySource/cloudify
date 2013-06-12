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
import java.util.Map;

import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 12:13 PM
 *
 * Resolver interface for retrieving service name and packed file.
 * <br/>
 *
 * @since 2.6.0
 */
public interface NameAndPackedFileResolver {

    /**
     *
     * @return The service name.
     * @throws CLIStatusException Thrown in case of an error.
     */
    String getName() throws CLIStatusException;

    /**
     *
     * @return The final packed file to be uploaded to the rest client
     * @throws CLIStatusException Thrown in case of an error.
     */
    File getPackedFile() throws CLIStatusException;

    /**
     * Determines the planned number of instances for the installation per service.
     * @return the planned number of instances
     * @throws CLIStatusException Thrown in case of an error.
     */
    Map<String, Integer> getPlannedNumberOfInstancesPerService() throws CLIStatusException;
    
    /**
     * returns the recipe dsl file.
     * @return the planned number of instances
     * @throws CLIStatusException Thrown in case of an error.
     */
    Object getDSLObject() throws CLIStatusException;
}
