package com.gigaspaces.cloudify.dsl;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "template", clazz = CloudTemplate.class, allowInternalNode = true, allowRootNode = false, parent = "cloud2")
public class CloudTemplate {

	private String imageId;
    private String machineMemoryMB;
    private String hardwareId;
    
	public String getImageId() {
		return imageId;
	}
	public void setImageId(String imageId) {
		this.imageId = imageId;
	}
	public String getMachineMemoryMB() {
		return machineMemoryMB;
	}
	public void setMachineMemoryMB(String machineMemoryMB) {
		this.machineMemoryMB = machineMemoryMB;
	}
	public String getHardwareId() {
		return hardwareId;
	}
	public void setHardwareId(String hardwareId) {
		this.hardwareId = hardwareId;
	}

}
