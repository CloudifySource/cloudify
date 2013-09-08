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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class RegionUtilsTest {

	@Test
	public void testConvertAvailabilityZone2LocationId() throws Exception {
		String result;

		String availabilityZone1 = "us-east-1a";
		result = RegionUtils.convertAvailabilityZone2LocationId(availabilityZone1);
		assertThat(result, is("us-east-1"));

		String availabilityZone2 = "eu-west-1";
		result = RegionUtils.convertAvailabilityZone2LocationId(availabilityZone2);
		assertThat(result, is("eu-west-1"));
	}

	@Test
	public void testConvertLocationId2Region() throws Exception {
		String locationId = "eu-west-1";
		Region result = RegionUtils.convertLocationId2Region(locationId);
		assertThat(result, is(Region.getRegion(Regions.EU_WEST_1)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertLocationId2RegionShouldFails() throws Exception {
		String locationId = "eu-west-1a";
		Region result = RegionUtils.convertLocationId2Region(locationId);
		assertThat(result, is(Region.getRegion(Regions.EU_WEST_1)));
	}

	@Test
	public void testConvertAvailabilityZone2Region() throws Exception {
		Region result;

		String availabilityZone1 = "us-east-1a";
		result = RegionUtils.convertAvailabilityZone2Region(availabilityZone1);
		assertThat(result, is(Region.getRegion(Regions.US_EAST_1)));

		String availabilityZone2 = "eu-west-1";
		result = RegionUtils.convertAvailabilityZone2Region(availabilityZone2);
		assertThat(result, is(Region.getRegion(Regions.EU_WEST_1)));

	}
}
