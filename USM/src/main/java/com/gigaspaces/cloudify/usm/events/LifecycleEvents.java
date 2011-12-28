package com.gigaspaces.cloudify.usm.events;

import java.util.HashMap;
import java.util.Map;

public enum LifecycleEvents {

	PRE_SERVICE_START("preServiceStart"),
	INIT("init"),
	PRE_INSTALL("preInstall"),
	INSTALL("install"),
	POST_INSTALL("postInstall"),
	PRE_START("preStart"),
	POST_START("postStart"),
	PRE_STOP("preStop"),
	POST_STOP("postStop"),
	SHUTDOWN("shutdown"),
	PRE_SERVICE_STOP("preServiceStop");

	private LifecycleEvents(String... fileNames) {
		this.fileNames = fileNames;
	}

	private String[] fileNames;
	
	private static Map<String, LifecycleEvents> eventByFileName = createEventsByFileName();
	
	private static Map<String, LifecycleEvents> createEventsByFileName() {
		HashMap<String, LifecycleEvents> map = new HashMap<String, LifecycleEvents>();
		for (LifecycleEvents event : LifecycleEvents.values()) {
			String[] fileNames = event.fileNames;
			for (String name : fileNames) {
				map.put(name, event);
			}
		}
		return map;
	}
	public static LifecycleEvents getEventForFile(String fileName) {
		return eventByFileName.get(fileName);
	}
}
