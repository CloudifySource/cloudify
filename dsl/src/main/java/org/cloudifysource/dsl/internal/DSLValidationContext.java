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
******************************************************************************/
package org.cloudifysource.dsl.internal;

/**
 * This class holds context properties that might be required for DSL validation.  
 * @author noak
 * @since 2.2.0
 */
public class DSLValidationContext {
	
	private String filePath;
	
	/**
	 * Gets the path of the DSL file being validated.
	 * @return The path to the validated DSL file
	 */
	public String getFilePath() {
		return filePath;
	}
	
	/**
	 * Sets the path of the DSL file being validated.
	 * @param filePath The path to the validated DSL file
	 */
	public void setFilePath(final String filePath) {
		this.filePath = filePath;
	}

}
