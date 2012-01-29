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
 *        An implementation of {@link ConsoleWithPropsActions}, to be used on an non-interactive console.
 */
public class ConsoleWithPropsNonInteractive implements ConsoleWithPropsActions {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPromptInternal(final String currentAppName) {
		return ">>> ";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBrandingPropertiesResourcePath() {
		return "META-INF/shell/noninteractive.branding.properties";
	}

}