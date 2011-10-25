package com.gigaspaces.cloudify.usm.events;

public interface PreStartListener extends USMEvent {

	EventResult onPreStart(StartReason reason);
}
