package com.gigaspaces.cloudify.usm.events;

public interface PreServiceStartListener extends USMEvent{
	EventResult onPreServiceStart();
}
