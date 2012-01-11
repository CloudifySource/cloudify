package org.cloudifysource.usm.events;

public interface PreStartListener extends USMEvent {

	EventResult onPreStart(StartReason reason);
}
