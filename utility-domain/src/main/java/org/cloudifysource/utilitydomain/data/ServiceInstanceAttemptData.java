/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.utilitydomain.data;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**********
 * A space entry that indicates the current attempt number of a service instance installation. Each time a service
 * instance is started, it writes this entry to the space, incrementing the value if an entry for this instance already
 * exists.
 *
 * @author barakme
 * @since 2.6.0
 *
 */
@SpaceClass
public class ServiceInstanceAttemptData {

	private String uid;
	private Long gscPid;
	private String applicationName;
	private String serviceName;
	private Integer instanceId;
	private Integer currentAttemptNumber;

	public ServiceInstanceAttemptData() {

	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	@SpaceId(autoGenerate = true)
	public String getUid() {
		return uid;
	}

	public void setUid(final String uid) {
		this.uid = uid;
	}

	public Long getGscPid() {
		return gscPid;
	}

	public void setGscPid(final Long gscPid) {
		this.gscPid = gscPid;
	}

	public Integer getCurrentAttemptNumber() {
		return currentAttemptNumber;
	}

	public void setCurrentAttemptNumber(final Integer currentAttemptNumber) {
		this.currentAttemptNumber = currentAttemptNumber;
	}

	public Integer getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(final Integer instanceId) {
		this.instanceId = instanceId;
	}

	@Override
	public String toString() {
		return "ServiceInstanceAttemptData [uid=" + uid + ", gscPid=" + gscPid + ", applicationName=" + applicationName
				+ ", serviceName=" + serviceName + ", instanceId=" + instanceId + ", currentAttemptNumber="
				+ currentAttemptNumber + "]";
	}


}
