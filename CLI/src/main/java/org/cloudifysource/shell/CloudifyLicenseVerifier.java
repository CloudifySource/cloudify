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

package org.cloudifysource.shell;

import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;

import com.gigaspaces.internal.license.LicenseManager;
import com.gigaspaces.license.LicenseException;

/********************
 * Cloudify license validation implementation. Basically delegates license validation to the udnerlying XAP
 * implementation.
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public class CloudifyLicenseVerifier {

	/************
	 * Verifies the Cloudify license where this CLI is running.
	 * 
	 * @throws CLIException if the license is invalid or missing.
	 */
	public void verifyLicense()
			throws CLIException {

		final LicenseManager manager = new LicenseManager();
		try {
			manager.verifyLicenseIfExists();
		} catch (final LicenseException e) {
			throw new CLIStatusException("bad_license", e.getMessage());
		}

	}

}
