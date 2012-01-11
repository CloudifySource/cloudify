package org.cloudifysource.usm.events;

public interface ShutdownListener extends USMEvent {

	EventResult onShutdown();
	
}
