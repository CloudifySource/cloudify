package com.gigaspaces.cloudify.usm.events;

public interface ShutdownListener extends USMEvent {

	EventResult onShutdown();
	
}
