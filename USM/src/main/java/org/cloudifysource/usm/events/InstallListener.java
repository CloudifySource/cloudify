package org.cloudifysource.usm.events;

public interface InstallListener extends PreInstallListener, PostInstallListener {
	EventResult onInstall();
	
}
