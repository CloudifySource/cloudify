package com.gigaspaces.cloudify.usm.events;

public interface PreStopListener extends USMEvent {

	EventResult onPreStop(StopReason reason);
}
