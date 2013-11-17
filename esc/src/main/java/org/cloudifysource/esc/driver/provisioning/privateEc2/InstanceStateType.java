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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2;

/**
 * A representation of possible EC2 Instance States.<br />
 * The source documentation of Amazon is here: <a
 * href="http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-ItemType-InstanceStateType.html"
 * >http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-ItemType-InstanceStateType.html</a>
 * 
 * @author victor
 * @since 2.7.0
 */
public enum InstanceStateType {
	/** Pending state. */
	PENDING("pending", 0),
	/** Running state. */
	RUNNING("running", 16),
	/** Shutting down state. */
	SHUTTING_DOWN("shutting-down", 32),
	/** Terminated state. */
	TERMINATED("terminated", 48),
	/** Stopping state. */
	STOPPING("stopping", 64),
	/** Stopped state. */
	STOPPED("stopped", 80);

	private static final int PENDING_CODE = 0;
	private static final int RUNNING_CODE = 16;
	private static final int SHUTTING_DOWN_CODE = 32;
	private static final int TERMINATED_CODE = 48;
	private static final int STOPPING_CODE = 64;
	private static final int STOPPED_CODE = 80;

	private String name;
	private int code;

	InstanceStateType(final String name, final int code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public int getCode() {
		return code;
	}

	/**
	 * Get an InstanceStateType object from a code.
	 * 
	 * @param code
	 *            The code of the state.
	 * @return An InstanceStateType object corresponding to the given code.
	 */
	public static InstanceStateType valueOf(final int code) {
		switch (code) {
		case PENDING_CODE:
			return PENDING;
		case RUNNING_CODE:
			return RUNNING;
		case SHUTTING_DOWN_CODE:
			return SHUTTING_DOWN;
		case TERMINATED_CODE:
			return TERMINATED;
		case STOPPING_CODE:
			return STOPPING;
		case STOPPED_CODE:
		default:
			return STOPPED;
		}
	}

}
