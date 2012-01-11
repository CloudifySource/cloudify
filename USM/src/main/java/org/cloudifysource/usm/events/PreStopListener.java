package org.cloudifysource.usm.events;

public interface PreStopListener extends USMEvent {

	EventResult onPreStop(StopReason reason);
}
