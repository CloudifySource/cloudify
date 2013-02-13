package org.cloudifysource.esc.driver.provisioning.storage;

/**
 * Describes a Block Storage Volume started by the {@link StorageProvisioningDriver}.
 * @author elip
 *
 */
public class VolumeDetails {
	
	private String id;
	private int size;
	private String location;
	private String name;
	
	public String getId() {
		return id;
	}
	public void setId(final String id) {
		this.id = id;
	}
	public int getSize() {
		return size;
	}
	public void setSize(final int size) {
		this.size = size;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(final String location) {
		this.location = location;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

}
