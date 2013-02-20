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

package org.cloudifysource.dsl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "isolationSLA", clazz = IsolationSLA.class, 
			allowInternalNode = true, allowRootNode = true, parent = "service")
public class IsolationSLA implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
			
	private GlobalIsolationSLADescriptor global;
	private DedicatedIsolationSLADescriptor dedicated;
	private AppSharedIsolationSLADescriptor appShared;
	private TenantSharedIsolationSLADescriptor tenantShared;
	
	public AppSharedIsolationSLADescriptor getAppShared() {
		return appShared;
	}

	public void setAppShared(final AppSharedIsolationSLADescriptor appShared) {
		this.appShared = appShared;
	}

	public TenantSharedIsolationSLADescriptor getTenantShared() {
		return tenantShared;
	}

	public void setTenantShared(final TenantSharedIsolationSLADescriptor tenantShared) {
		this.tenantShared = tenantShared;
	}

	public GlobalIsolationSLADescriptor getGlobal() {
		return global;
	}

	public void setGlobal(final GlobalIsolationSLADescriptor global) {
		this.global = global;
	}

	public DedicatedIsolationSLADescriptor getDedicated() {
		return dedicated;
	}

	public void setDedicated(final DedicatedIsolationSLADescriptor dedicated) {
		this.dedicated = dedicated;
	}
	
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		
		List<Object> notNulls = new ArrayList<Object>();
		
		if (global != null) {
			notNulls.add(global);
		}
		if (dedicated != null) {
			notNulls.add(global);
		}
		if (appShared != null) {
			notNulls.add(global);
		}
		if (tenantShared != null) {
			notNulls.add(global);
		}
		
		if (notNulls.size() > 1) {
			throw new DSLValidationException("cannot define two types of isolation sla's. please choose one");
		}
	}

	@Override
	public String toString() {
		return "IsolationSLA [global=" + global + ", dedicated=" + dedicated
				+ ", appShared=" + appShared + ", tenantShared=" + tenantShared
				+ "]";
	}
}
