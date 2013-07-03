package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * This class defines a service deployment which is shared across all machines 
 * that are provisioned for the same application in the cluster.
 * Each service instance will be allocated according to the requirements specified.
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "appShared", clazz = AppSharedIsolationSLADescriptor.class, allowInternalNode = true,
	allowRootNode = false, parent = "isolationSLA")
public class AppSharedIsolationSLADescriptor extends SharedIsolationSLADescriptor {


	
	@Override
	public String toString() {
		return "AppSharedIsolationSLADescriptor [instanceMemoryMB="
				+ getInstanceMemoryMB() + ", instanceCpuCores=" + getInstanceCpuCores()
				+ ", useManagement=" + isUseManagement() + "]";
	}
}
