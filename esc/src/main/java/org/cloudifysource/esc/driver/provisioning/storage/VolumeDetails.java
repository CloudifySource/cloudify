package org.cloudifysource.esc.driver.provisioning.storage;

/**
 * Describes a Block Storage Volume started by the {@link StorageProvisioningDriver}.
 * @author elip
 *
 */
public class VolumeDetails {
	
	private String id;
	private String name;
	private int size;
	private String location;
	
	/**
	 * Gets the volume id.
	 * @return volume id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the volume id.
	 * @param id The id to set on the volume
	 */
	public void setId(final String id) {
		this.id = id;
	}
	
	/**
	 * Gets the volume name.
	 * @return the volume name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the volume name.
	 * @param name Volume name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * Gets the volume size.
	 * @return the volume size
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * Sets the volume size.
	 * @param size the volume size to set
	 */
	public void setSize(final int size) {
		this.size = size;
	}
	
	/**
	 * Gets the location.
	 * @return the volume location
	 */
	public String getLocation() {
		return location;
	}
	
	/**
	 * Sets the volume location.
	 * @param location Volume location to set
	 */
	public void setLocation(final String location) {
		this.location = location;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "VolumeDetails [volumeId=" + id + ", name=" + name + ", size=" + size + ", location=" + location + "]";
	}

}
