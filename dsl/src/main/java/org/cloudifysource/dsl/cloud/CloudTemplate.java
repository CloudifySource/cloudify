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
package org.cloudifysource.dsl.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * @author barakme
 * @since 2.0.0
 * 
 *        A cloud template is a group of settings that define a given configuration, available for a specific
 *        cloud. It can include physical machine properties (e.g. memory), operating system type, location,
 *        available cloud nodes and other settings.
 */
@CloudifyDSLEntity(name = "template", clazz = CloudTemplate.class, allowInternalNode = true, allowRootNode = false,
	parent = "cloud")
public class CloudTemplate {

	private String imageId;
	private int machineMemoryMB;
	private String hardwareId;
	private String locationId;

	private int numberOfCores = 1;

	private Map<String, Object> options = new HashMap<String, Object>();
	private Map<String, Object> overrides = new HashMap<String, Object>();
	private List<Map<String, String>> nodesList = new ArrayList<Map<String, String>>();

	/**
	 * Gets the image ID.
	 * 
	 * @return The image ID
	 */
	public String getImageId() {
		return imageId;
	}

	/**
	 * Sets the image ID.
	 * 
	 * @param imageId
	 *            The ID of the image to use
	 */
	public void setImageId(final String imageId) {
		this.imageId = imageId;
	}

	/**
	 * Gets the machine memory size in MB.
	 * 
	 * @return The machine memory size
	 */
	public int getMachineMemoryMB() {
		return machineMemoryMB;
	}

	/**
	 * Sets the machine memory size in MB.
	 * 
	 * @param machineMemoryMB
	 *            The machine memory size
	 */
	public void setMachineMemoryMB(final int machineMemoryMB) {
		this.machineMemoryMB = machineMemoryMB;
	}

	/**
	 * Gets the hardware ID.
	 * 
	 * @return The ID of the hardware profile
	 */
	public String getHardwareId() {
		return hardwareId;
	}

	/**
	 * Sets the hardware ID.
	 * 
	 * @param hardwareId
	 *            the ID of the hardware profile
	 */
	public void setHardwareId(final String hardwareId) {
		this.hardwareId = hardwareId;
	}

	/**
	 * Gets the location ID.
	 * 
	 * @return The location ID
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Sets the location ID.
	 * 
	 * @param locationId
	 *            The ID of this location
	 */
	public void setLocationId(final String locationId) {
		this.locationId = locationId;
	}

	/**
	 * Gets the machine's cores' number.
	 * 
	 * @return The machine's cores' number
	 */
	public int getNumberOfCores() {
		return numberOfCores;
	}

	/**
	 * Sets the number of cores on this machine.
	 * 
	 * @param numberOfCores
	 *            The machine's cores' number
	 */
	public void setNumberOfCores(final int numberOfCores) {
		this.numberOfCores = numberOfCores;
	}

	/**
	 * Gets the configured options.
	 * 
	 * @return A map of configured options
	 */
	public Map<String, Object> getOptions() {
		return options;
	}

	/**
	 * Sets optional settings.
	 * 
	 * @param options
	 *            A map of optional settings
	 */
	public void setOptions(final Map<String, Object> options) {
		this.options = options;
	}

	/**
	 * Gets the configured overrides.
	 * 
	 * @return A list of configured overrides
	 */
	public Map<String, Object> getOverrides() {
		return overrides;
	}

	/**
	 * Sets overriding settings. This is optional.
	 * 
	 * @param overrides
	 *            A map of overriding settings
	 */
	public void setOverrides(final Map<String, Object> overrides) {
		this.overrides = overrides;
	}

	/**
	 * Gets the configured cloud nodes.
	 * 
	 * @return A list of configured cloud nodes
	 */
	public List<Map<String, String>> getNodesList() {
		return nodesList;
	}

	/**
	 * Sets cloud nodes that are available for this template.
	 * 
	 * @param nodesList
	 *            A list of cloud nodes that are available for this template
	 */
	public void setNodesList(final List<Map<String, String>> nodesList) {
		this.nodesList = nodesList;
	}

	@Override
	public String toString() {
		return "CloudTemplate [imageId=" + imageId + ", machineMemoryMB=" + machineMemoryMB + ", hardwareId="
				+ hardwareId + ", locationId=" + locationId + ", numberOfCores=" + numberOfCores + ", options="
				+ options + ", overrides=" + overrides + ", nodesList=" + nodesList + "]";
	}

}
