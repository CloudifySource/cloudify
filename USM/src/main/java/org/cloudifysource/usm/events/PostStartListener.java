package org.cloudifysource.usm.events;

public interface PostStartListener extends USMEvent {

	EventResult onPostStart(StartReason reason);
}
