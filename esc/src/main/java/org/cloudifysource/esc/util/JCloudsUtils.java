package org.cloudifysource.esc.util;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.AvailabilityZoneInfo;
import org.jclouds.ec2.services.AvailabilityZoneAndRegionClient;

/**
 * JClouds related utilities.
 * @author noak
 * @since 2.5.0
 */
public final class JCloudsUtils {


	// hidden constructor
	private JCloudsUtils() { }
	
	/**
	 * Gets the name of the region for the given location id (region or availability zone).
	 * @param ec2Client Jclouds' EC2Client object
	 * @param locationId Id of a region or availability zone
	 * @return Region name
	 * @throws CloudProvisioningException Indicates an invalid location
	 */
	public static String getEC2region(final EC2Client ec2Client, final String locationId) 
			throws CloudProvisioningException {
		
		String region = "";
		
		Set<String> knownRegions = ec2Client.getConfiguredRegions();
		
		if (knownRegions.contains(locationId)) {
			region = locationId;
		} else {
			//the location doesn't specify a known region, might be an availability zone
			AvailabilityZoneAndRegionClient locationClient = ec2Client.getAvailabilityZoneAndRegionServices();
			for (String knownRegion : knownRegions) {
				for (AvailabilityZoneInfo zoneInfo : locationClient.describeAvailabilityZonesInRegion(knownRegion)) {
					if (zoneInfo.getZone().equalsIgnoreCase(locationId)) {
						//the location id specifies this availablity zone
						region = zoneInfo.getRegion();
						break;
					}
				}
				
				if (!StringUtils.isBlank(region)) {
					break;
				}
			}
		}
		
		if (StringUtils.isBlank(region)) {
			throw new CloudProvisioningException("Invalid region or availability zone: " + locationId);
		}
		return region;
	}
}
