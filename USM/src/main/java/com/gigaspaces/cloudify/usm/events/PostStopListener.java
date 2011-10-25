package com.gigaspaces.cloudify.usm.events;

public interface PostStopListener extends USMEvent {

	EventResult onPostStop(StopReason reason);
}
