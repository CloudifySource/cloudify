package org.cloudifysource.dsl;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

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

	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		super.validateDefaultValues(validationContext);
		
		if (isUseManagement()) {
			throw new DSLValidationException("isUseManagement can only be true for isolationSLA of type 'global'");
		}
		
	}
	
	@Override
	public String toString() {
		return "AppSharedIsolationSLADescriptor [instanceMemoryMB="
				+ getInstanceMemoryMB() + ", instanceCpuCores=" + getInstanceCpuCores()
				+ ", useManagement=" + isUseManagement() + "]";
	}
}
