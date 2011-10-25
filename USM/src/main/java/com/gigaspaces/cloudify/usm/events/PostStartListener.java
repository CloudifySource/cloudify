package com.gigaspaces.cloudify.usm.events;

public interface PostStartListener extends USMEvent {

	EventResult onPostStart(StartReason reason);
}
