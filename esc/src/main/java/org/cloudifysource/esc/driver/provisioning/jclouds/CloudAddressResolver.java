/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

/************
 * Helper class for the default cloud driver that resolves the 'private' and 'public' node IPs, as Cloudify defined
 * private and public, according to CIDR blocks and regex filters.
 *
 * @author barakme
 * @since 2.6.0
 *
 */
public class CloudAddressResolver {

	/*****************
	 * Resolves an address, based on a primary and secondary set of addresses, a CIDR subnet description and a regex.
	 * @param addresses the primary list of addresses.
	 * @param backupAddresses the secondary list of addresses.
	 * @param subnetInfo the CIDR defined subnet.
	 * @param regex the regular expression.
	 * @return the resolved IP, which may be null.
	 */
	public String getAddress(final Set<String> addresses, final Set<String> backupAddresses,
			final SubnetInfo subnetInfo, final Pattern regex) {
		if (subnetInfo != null) {
			for (final String address : addresses) {
				if (subnetInfo.isInRange(address)) {
					return address;
				}
			}
		}

		if (regex != null) {
			for (final String address : addresses) {
				if (regex.matcher(address).matches()) {
					return address;
				}
			}

		}

		if (subnetInfo != null) {
			for (final String address : backupAddresses) {
				if (subnetInfo.isInRange(address)) {
					return address;
				}
			}
		}

		if (regex != null) {
			for (final String address : backupAddresses) {
				if (regex.matcher(address).matches()) {
					return address;
				}
			}
		}

		if (addresses.size() > 0) {
			return addresses.iterator().next();
		}

		return null;
	}

}