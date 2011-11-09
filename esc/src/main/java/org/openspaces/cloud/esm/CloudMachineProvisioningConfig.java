package org.openspaces.cloud.esm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.util.StringProperties;
import org.openspaces.grid.gsm.capacity.CapacityRequirement;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.DriveCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;

/***********
 * Configuration object for the cloud ESM machine provisioning.
 * 
 * @author barakme
 * 
 */
public class CloudMachineProvisioningConfig implements ElasticMachineProvisioningConfig {

	private static final String ZONES_KEY = "zones";
	private static final String ZONES_SEPARATOR = ",";
	private static final String[] ZONES_DEFAULT = new String[] {"agent"};
	private static final String LOCATOR_KEY = "locator";
	private static final String MEMORY_MEGABYTES_PER_MACHINE_KEY = "machine-memory-in-mb-per-machine";
	private static final String API_KEY_KEY = "api-key";
	private static final String MACHINE_NAME_PREFIX_KEY = "machine-name-prefix";
	private static final String GRID_NAME_KEY = "grid-name";
	private static final String IMAGE_ID_KEY = "image-id";
	private static final String PROVIDER_KEY = "provider";
	private static final String USER_KEY = "user";
	private static final String LOCAL_DIRECTORY_KEY = "local-directory";
	private static final String LOCAL_DIRECTORY_DEFAULT = "c:/docBase";
	private static final String REMOTE_DIRECTORY_KEY = "remote-directory";
	private static final String HARDWARE_ID_DIRECTORY_KEY = "hardware-id";
	private static final String HARDWARE_ID_DEFAULT = "";
	private static final String LOCATION_ID_KEY = "location-id";
	private static final String LOCATION_ID_DEFAULT = "us-east-1d";
	private static final String KEY_PAIR_DIRECTORY_KEY = "key-pair";
	private static final String KEY_PAIR_DEFAULT = "";
	private static final String KEY_FILE_DIRECTORY_KEY = "key-file";
	private static final String KEY_FILE_DEFAULT = "";
	private static final String SECURITY_GROUP_DIRECTORY_KEY = "security-group";
	private static final String SECURITY_GROUP_DEFAULT = "default";
	private static final String REMOTE_DIRECTORY_DEFAULT = "/opt";
	private static final long MIN_RAM_MEGABYTES_DEFAULT = 1024;
	private static final String NUMBER_OF_MANAGEMENT_MACHINES_KEY = "number-of-management-machines";
	private static final int NUMBER_OF_MANAGEMENT_MACHINES_DEFAULT = 2;
	private static final String MANAGEMENT_GROUP_KEY = "management-group";
	private static final String MANAGEMENT_GROUP_DEFAULT = "management_machine";
	private static final boolean DEDICATED_MANAGEMENT_MACHINES_DEFAULT = false;
	private static final String DEDICATED_MANAGEMENT_MACHINES_KEY = "dedicated-management-machines";
	private static final boolean CONNECTED_TO_PRIVATE_IP_DEFAULT = false;
	private static final String CONNECTED_TO_PRIVATE_IP_KEY = "connected-to-private-ip";
	private static final double NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT = 4;
	private static final String NUMBER_OF_CPU_CORES_PER_MACHINE_KEY = "number-of-cpu-cores-per-machine";
	private static final String RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY =
			"reserved-memory-capacity-per-machine-megabytes";
	private static final long RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT = 256;
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY =
			"resereved-drives-capacity-per-machine-megabytes";
	private static final Map<String, String> RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT =
			new HashMap<String, String>();
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR = "=";
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR = ",";
	private static final String RESERVED_CPU_PER_MACHINE_KEY = "reserved-cpu-cores-per-machine";
	private static final double RESERVED_CPU_PER_MACHINE_DEFAULT = 0.0;
	private static final String MANAGEMENT_ONLY_FILES_KEY = "management-only-files";
	private static final String MANAGEMENT_ONLY_FILES_SEPARATOR = ",";
	private static final String[] MANAGEMENT_ONLY_FILES_DEFAULT = new String[]{};
	private static final String GROUPS_KEY = "com.gs.jini_lus.groups";
	private static final String CLOUDIFY_URL_KEY = "cloudify-url";
	private static final String SSH_LOGGING_LEVEL_DEFAULT = Level.WARNING.toString();
	private static final String SSH_LOGGING_LEVEL_KEY = "ssh-logging-level";
	
	private StringProperties properties;

	
	public double getMinimumNumberOfCpuCoresPerMachine() {
		return properties.getDouble(NUMBER_OF_CPU_CORES_PER_MACHINE_KEY,
				NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT);
	}

	/* CHECKSTYLE:OFF */
	public void setMinimumNumberOfCpuCoresPerMachine(final double minimumCpuCoresPerMachine) {
		properties.putDouble(NUMBER_OF_CPU_CORES_PER_MACHINE_KEY, minimumCpuCoresPerMachine);
	}

	public CloudMachineProvisioningConfig() {
		this.properties = new StringProperties(new HashMap<String, String>());
	}
	
	public CloudMachineProvisioningConfig(final Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}

	public CloudMachineProvisioningConfig(final Properties properties) {
		this.properties = new StringProperties(properties);
	}

	
	public String getBeanClassName() {
		return CloudMachineProvisioning.class.getName();
	}

	public void setProperties(final Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}

	public Map<String, String> getProperties() {
		return this.properties.getProperties();
	}

	/**
	 * Sets the expected amount of memory per machine that is reserved for
	 * processes other than grid containers. These include Grid Service Manager,
	 * Lookup Service or any other daemon running on the system.
	 * 
	 * Default value is 1024 MB.
	 * 
	 * For example, by default, a 16GB server can run 3 containers 5GB each,
	 * since it approximately leaves 1024MB memory free.
	 * 
	 * @param reservedInMB
	 *            - amount of reserved memory in MB
	 * 
	 * @since 8.0.1
	 * @see #setReservedCapacityPerMachine(CapacityRequirements)
	 */
	public void setReservedMemoryCapacityPerMachineInMB(final long reservedInMB) {
		this.properties.putLong(RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY, reservedInMB);
	}

	public Map<String, Long> getReservedDriveCapacityPerMachineInMB() {
		final Map<String, String> reserved =
				this.properties.getKeyValuePairs(
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR,
						RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT);

		final Map<String, Long> reservedInMB = new HashMap<String, Long>();
		for(final Map.Entry<String, String> entry : reserved.entrySet()){
            String drive = entry.getKey();
			reservedInMB.put(drive, Long.valueOf(entry.getValue()));
		}

		return reservedInMB;

	}

	/**
	 * Sets the expected amount of disk drive size per machine that is reserved
	 * for processes other than grid containers. These include Grid Service
	 * Manager, Lookup Service or any other daemon running on the system.
	 * 
	 * Default value is 0 MB.
	 * 
	 * @param reservedInMB
	 *            - amount of reserved disk drive in MB
	 * 
	 * @since 8.0.3
	 * @see #setReservedCapacityPerMachine(CapacityRequirements)
	 */
	public void setReservedDriveCapacityPerMachineInMB(final Map<String, Long> reservedInMB) {
		final Map<String, String> reservedInString = new HashMap<String, String>();
		for(final Map.Entry<String, Long> entry : reservedInMB.entrySet()){
			String drive = entry.getKey();
			reservedInString.put(drive, entry.getValue().toString());
		}
		this.properties.putKeyValuePairs(RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY, reservedInString,
				RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR,
				RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR);
	}

	
	public long getReservedMemoryCapacityPerMachineInMB() {
		return this.properties.getLong(RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY,
				RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT);
	}

	public double getReservedCpuCapacityPerMachine() {
		return this.properties.getDouble(RESERVED_CPU_PER_MACHINE_KEY, RESERVED_CPU_PER_MACHINE_DEFAULT);
	}

	/**
	 * Sets the expected CPU cores per machine that is reserved for processes
	 * other than grid containers. These include Grid Service Manager, Lookup
	 * Service or any other daemon running on the system.
	 * 
	 * Default value is 0 cpu cores.
	 * 
	 * @param reservedCpu
	 *            - number of reserved CPU cores
	 * 
	 * @since 8.0.3
	 * @see #setReservedCapacityPerMachine(CapacityRequirements)
	 */
	public void setReservedCpuCapacityPerMachineInMB(final double reservedCpu) {
		this.properties.putDouble(RESERVED_CPU_PER_MACHINE_KEY, reservedCpu);
	}

	public CapacityRequirements getReservedCapacityPerMachine() {
		final List<CapacityRequirement> requirements = new ArrayList<CapacityRequirement>();
		requirements.add(new MemoryCapacityRequirement(getReservedMemoryCapacityPerMachineInMB()));
		requirements.add(new CpuCapacityRequirement(getReservedCpuCapacityPerMachine()));
		final Map<String, Long> reservedDriveCapacity = getReservedDriveCapacityPerMachineInMB();
		for (final Entry<String, Long> entry : reservedDriveCapacity.entrySet()) {
			String drive = entry.getKey(); 
			requirements.add(new DriveCapacityRequirement(drive, entry.getValue()));
		}
		return new CapacityRequirements(requirements.toArray(new CapacityRequirement[requirements.size()]));
	}

	/**
	 * Sets the expected amount of memory, cpu, drive space (per machine) that
	 * is reserved for processes other than processing units. These include Grid
	 * Service Manager, Lookup Service or any other daemon running on the
	 * system.
	 * 
	 * Default value is 1024 MB RAM. For example, by default, a 16GB server can
	 * run 3 containers 5GB each, since it approximately leaves 1024MB memory
	 * free.
	 * 
	 * @param capacityRequirements
	 *            - specifies the reserved memory/cpu/disk space
	 * 
	 * @since 8.0.2
	 * @see #setReservedCpuCapacityPerMachineInMB(double)
	 * @see #setReservedMemoryCapacityPerMachineInMB(long)
	 * @see #setReservedDriveCapacityPerMachineInMB(Map)
	 */
	public void setReservedCapacityPerMachine(final CapacityRequirements capacityRequirements) {

		final MemoryCapacityRequirement memoryCapacityRequirement =
				capacityRequirements.getRequirement(new MemoryCapacityRequirement().getType());
		if (!memoryCapacityRequirement.equalsZero()) {
			setReservedMemoryCapacityPerMachineInMB(memoryCapacityRequirement.getMemoryInMB());
		}

		final CpuCapacityRequirement cpuCapacityRequirement =
				capacityRequirements.getRequirement(new CpuCapacityRequirement().getType());
		if (!memoryCapacityRequirement.equalsZero()) {
			setReservedCpuCapacityPerMachineInMB(cpuCapacityRequirement.getCpu().doubleValue());
		}

		final Map<String, Long> reservedInMB = new HashMap<String, Long>();
		for (final CapacityRequirement requirement : capacityRequirements.getRequirements()) {
			if (requirement instanceof DriveCapacityRequirement) {
				final DriveCapacityRequirement driveRequirement = (DriveCapacityRequirement) requirement;
				reservedInMB.put(driveRequirement.getDrive(), driveRequirement.getDriveCapacityInMB());
			}
		}
		setReservedDriveCapacityPerMachineInMB(reservedInMB);
	}

	public String[] getGridServiceAgentZones() {
		return properties.getArray(ZONES_KEY, ZONES_SEPARATOR, ZONES_DEFAULT);
	}

	public void setManagementGroup(final String value) {
	    properties.put(MANAGEMENT_GROUP_KEY, value);
	}
	
	public String getManagementGroup() {
	    return properties.get(MANAGEMENT_GROUP_KEY, MANAGEMENT_GROUP_DEFAULT);
	}
	
	public void setNumberOfManagementMachines(int value) {
	    properties.putInteger(NUMBER_OF_MANAGEMENT_MACHINES_KEY, value);
	}
	
	public int getNumberOfManagementMachines() {
	    return properties.getInteger(NUMBER_OF_MANAGEMENT_MACHINES_KEY, NUMBER_OF_MANAGEMENT_MACHINES_DEFAULT);
	}
	
	public void setsDedicatedManagementMachines(final boolean value) {
		properties.putBoolean(DEDICATED_MANAGEMENT_MACHINES_KEY, value);
	}
	
	public boolean isDedicatedManagementMachines() {
		return properties.getBoolean(DEDICATED_MANAGEMENT_MACHINES_KEY, DEDICATED_MANAGEMENT_MACHINES_DEFAULT);
	}

	public void setConnectedToPrivateIp(final boolean value) {
	    properties.putBoolean(CONNECTED_TO_PRIVATE_IP_KEY, value);
	}
	
	public boolean isConnectedToPrivateIp() {
	    return properties.getBoolean(CONNECTED_TO_PRIVATE_IP_KEY, CONNECTED_TO_PRIVATE_IP_DEFAULT);
	}
	
	public void setSshLoggingLevel(final Level level) {
	    properties.put(SSH_LOGGING_LEVEL_KEY, level.toString());
	}
	
	public Level getSshLoggingLevel() {
	    String level = properties.get(SSH_LOGGING_LEVEL_KEY, SSH_LOGGING_LEVEL_DEFAULT);
	    return Level.parse(level);
	}
	
	public boolean isGridServiceAgentZoneMandatory() {
		return false;
	}

	public void setUser(final String user) {
		properties.put(USER_KEY, user);
	}

	public String getUser() {
		return properties.get(USER_KEY);
	}

	public void setApiKey(final String apiKey) {
		properties.put(API_KEY_KEY, apiKey);
	}

	public String getApiKey() {
		return properties.get(API_KEY_KEY);
	}

	public void setMachineNamePrefix(final String machineNamePrefix) {
		properties.put(MACHINE_NAME_PREFIX_KEY, machineNamePrefix);
	}

	public String getMachineNamePrefix() {
		return properties.get(MACHINE_NAME_PREFIX_KEY);
	}

	public void setGridName(final String gridName) {
		properties.put(GRID_NAME_KEY, gridName);
	}

	public String getGridName() {
		return properties.get(GRID_NAME_KEY);
	}

	public void setProvider(final String provider) {
		properties.put(PROVIDER_KEY, provider);
	}

	public String getProvider() {
		return properties.get(PROVIDER_KEY);
	}

	public void setImageId(final String imageId) {
		properties.put(IMAGE_ID_KEY, imageId);
	}

	public String getImageId() {
		return properties.get(IMAGE_ID_KEY);
	}

	public void setMachineMemoryMB(final long minRamMegabytes) {
		properties.putLong(MEMORY_MEGABYTES_PER_MACHINE_KEY, minRamMegabytes);
	}

	public long getMachineMemoryMB() {
		return properties.getLong(MEMORY_MEGABYTES_PER_MACHINE_KEY, MIN_RAM_MEGABYTES_DEFAULT);
	}

	public void setLocator(final String locator) {
		properties.put(LOCATOR_KEY, locator);
	}

	public String getLocator() {
		return properties.get(LOCATOR_KEY);
	}

	public String getLocalDirectory() {
		return properties.get(LOCAL_DIRECTORY_KEY, LOCAL_DIRECTORY_DEFAULT);
	}

	public void setLocalDirectory(final String localDir) {
		properties.put(LOCAL_DIRECTORY_KEY, localDir);
	}

	public String getRemoteDirectory() {
		return properties.get(REMOTE_DIRECTORY_KEY, REMOTE_DIRECTORY_DEFAULT);
	}

	public void setRemoteDirectory(final String remoteDir) {
		properties.put(REMOTE_DIRECTORY_KEY, remoteDir);
	}

	public String getHardwareId() {
		return properties.get(HARDWARE_ID_DIRECTORY_KEY, HARDWARE_ID_DEFAULT);
	}

	public void setHardwareId(final String hardwareId) {
		properties.put(HARDWARE_ID_DIRECTORY_KEY, hardwareId);
	}

   public String getLocationId() {
        return properties.get(LOCATION_ID_KEY, LOCATION_ID_DEFAULT);
    }
    
    public void setLocationId(final String locationId) {
        properties.put(LOCATION_ID_KEY, locationId);
    }
	
	public String getSecurityGroup() {
		return properties.get(SECURITY_GROUP_DIRECTORY_KEY, SECURITY_GROUP_DEFAULT);
	}
	
	public void setSecurityGroup(final String securityGroup) {
		properties.put(SECURITY_GROUP_DIRECTORY_KEY, securityGroup);
	}

	public String getKeyPair() {
		return properties.get(KEY_PAIR_DIRECTORY_KEY, KEY_PAIR_DEFAULT);
	}

	public void setKeyPair(final String keyPair) {
		properties.put(KEY_PAIR_DIRECTORY_KEY, keyPair);
	}

	public String getKeyFile() {
		return properties.get(KEY_FILE_DIRECTORY_KEY, KEY_FILE_DEFAULT);
	}

	public void setKeyFile(final String keyFile) {
		properties.put(KEY_FILE_DIRECTORY_KEY, keyFile);
	}

	public String getCloudifyUrl() {
	    return properties.get(CLOUDIFY_URL_KEY);
	}
	
	public void setCloudifyUrl(final String cloudifyUrl) {
	    properties.put(CLOUDIFY_URL_KEY, cloudifyUrl);
	}
	
	/* CHECKSTYLE:ON */

	public List<String> getManagementOnlyFiles() {
		String[] managmentFilesArray = properties.getArray(MANAGEMENT_ONLY_FILES_KEY, MANAGEMENT_ONLY_FILES_SEPARATOR, MANAGEMENT_ONLY_FILES_DEFAULT);
		List<String> managmentFilesList = new ArrayList<String>();
		
		for (String str : managmentFilesArray) {
			managmentFilesList.add(str);
		}
		return managmentFilesList;
		
	}

	
	public void setManagementOnlyFiles(List<String> managmentOnlyFiles) {
		String[] managmentOnlyFilesArray = (String[]) managmentOnlyFiles.toArray(new String[0]);
		properties.putArray(MANAGEMENT_ONLY_FILES_KEY, managmentOnlyFilesArray, MANAGEMENT_ONLY_FILES_SEPARATOR);
	}

	public void setZones(final List<String> zones) {
		String[] zonesArray = (String[]) zones.toArray(new String[0]);
		properties.putArray(ZONES_KEY, zonesArray, ZONES_SEPARATOR);
	}

	public List<String> getZones() {
		String[] zonesArray = properties.getArray(ZONES_KEY, ZONES_SEPARATOR, ZONES_DEFAULT);
		List<String> zonesList = new ArrayList<String>();
		
		for (String str : zonesArray) {
			zonesList.add(str);
		}
		return zonesList;
	}
	
	public String getGroups() {
		return properties.get(GROUPS_KEY);
	}
	
}
