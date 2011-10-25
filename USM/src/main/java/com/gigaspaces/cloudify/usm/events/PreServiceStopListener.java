package com.gigaspaces.cloudify.usm.events;

public interface PreServiceStopListener extends USMEvent{
	EventResult onPreServiceStop();
}
