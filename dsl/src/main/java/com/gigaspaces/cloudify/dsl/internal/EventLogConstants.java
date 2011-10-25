package com.gigaspaces.cloudify.dsl.internal;

public class EventLogConstants {
	private static String timeStamp = "timeStamp";
	private static String machineHostName = "machineHostName";
	private static String machineHostAddress = "machineHostAddress";
	private static String serviceName = "serviceName";
	private static String eventText = "eventText";
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
