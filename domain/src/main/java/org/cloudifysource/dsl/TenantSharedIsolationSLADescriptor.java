package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * This class defines a service deployment which is shared across all machines that are provisioned for the same tenant
 * in the cluster. Each service instance will be allocated according to the requirements specified.
 *
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "tenantShared", clazz = TenantSharedIsolationSLADescriptor.class, allowInternalNode = true,
		allowRootNode = false, parent = "isolationSLA")
public class TenantSharedIsolationSLADescriptor extends SharedIsolationSLADescriptor {

	@Override
	public String toString() {
		return "TenantSharedIsolationSLADescriptor [instanceMemoryMB="
				+ getInstanceMemoryMB() + ", instanceCpuCores=" + getInstanceCpuCores()
				+ ", useManagement=" + isUseManagement() + "]";
	}

}
