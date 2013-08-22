 /* Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.rest.response;

/**
 * Describes the possible response statuses of add-templates operation.
 * @author yael
 *
 */
public enum AddTemplatesStatus {
 
	/**
	 * All the templates were successfully added to all the REST instances.
	 */
	SUCCESS("Success"),
	
	/**
	 * At least one template failed to be added to at least one REST instance.
	 */
    PARTIAL_FAILURE("PartialFailure"),

    /**
     * All templates failed to be added to all REST instances.
     */
    FAILURE("Failed");

    
    private final String name;

    AddTemplatesStatus(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
