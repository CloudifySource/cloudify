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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import org.apache.commons.lang.math.NumberUtils;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * Utility class for region, availability zone and location id conversion.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public final class RegionUtils {

	private RegionUtils() {
	}

	/**
	 * Convert an availability zone to com.amazonaws.regions.Region.
	 * 
	 * @param availabilityZone
	 *            The availability zone to convert (i.e. us-east-1a, us-east-1b, ...).
	 * @return The converted com.amazonaws.regions.Region.
	 */
	public static Region convertAvailabilityZone2Region(final String availabilityZone) {
		Region region;
		String regionStr = availabilityZone;
		String lastChar = availabilityZone.substring(availabilityZone.length() - 1, availabilityZone.length());
		if (!NumberUtils.isDigits(lastChar)) {
			regionStr = availabilityZone.substring(0, availabilityZone.length() - 1);

		}
		region = Region.getRegion(Regions.valueOf(regionStr.replaceAll("-", "_").toUpperCase()));
		return region;
	}

	/**
	 * Convert a location id to com.amazonaws.regions.Region.
	 * 
	 * @param locationId
	 *            The location id to convert (i.e. us-east-1)
	 * @return The converted com.amazonaws.regions.Region.
	 */
	public static Region convertLocationId2Region(final String locationId) {
		String regionString = locationId.replaceAll("-", "_").toUpperCase();
		Regions regionEnum = Regions.valueOf(regionString);
		Region region = Region.getRegion(regionEnum);
		return region;
	}

	/**
	 * Convert an availability zone to location id.
	 * 
	 * @param availabilityZone
	 *            The availability zone to convert (i.e. us-east-1a, us-east-1b, ...)
	 * @return The converted location id.
	 */
	public static String convertAvailabilityZone2LocationId(final String availabilityZone) {
		String locationId = availabilityZone;
		String lastChar = availabilityZone.substring(availabilityZone.length() - 1, availabilityZone.length());
		if (!NumberUtils.isDigits(lastChar)) {
			locationId = availabilityZone.substring(0, availabilityZone.length() - 1);
		}
		return locationId;
	}
}
