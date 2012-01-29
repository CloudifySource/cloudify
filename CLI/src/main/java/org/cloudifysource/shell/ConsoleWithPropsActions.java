/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        This interface define two methods to be implemented, regarding the console's prompt text and the
 *        properties to be used.
 */
public interface ConsoleWithPropsActions {

	/**
	 * Gets the prompt to be used in this console. If the current application name is not null - it is
	 * embedded in the prompt.
	 * 
	 * @param currentAppName
	 *            The name of the current application.
	 * @return The prompt string
	 */
	String getPromptInternal(String currentAppName);

	/**
	 * Gets the path to the branding properties of this console.
	 * 
	 * @return The path to the properties file
	 */
	String getBrandingPropertiesResourcePath();

}
