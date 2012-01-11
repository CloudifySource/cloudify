package org.cloudifysource.usm.events;

public interface PostStopListener extends USMEvent {

	EventResult onPostStop(StopReason reason);
}
