package org.cloudifysource.usm.events;

public interface PreInstallListener extends USMEvent {

	EventResult onPreInstall();
}
