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
package org.cloudifysource.dsl.internal;

public class EventLogConstants {
	private static final String timeStamp = "timeStamp";
	private static final String machineHostName = "machineHostName";
	private static final String machineHostAddress = "machineHostAddress";
	private static final String serviceName = "serviceName";
	private static final String eventText = "eventText";
//	private static String ee = USMLifecycleBean

	public static String getTimeStampKey() {
		return timeStamp;
	}

	public static String getMachineHostNameKey() {
		return machineHostName;
	}

	public static String getMachineHostAddressKey() {
		return machineHostAddress;
	}

	public static String getServiceNameKey() {
		return serviceName;
	}

	public static String getEventTextKey() {
		return eventText;
	}
}
