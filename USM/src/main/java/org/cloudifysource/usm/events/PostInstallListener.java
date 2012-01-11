package org.cloudifysource.usm.events;

public interface PostInstallListener extends USMEvent {

	EventResult onPostInstall();
}
