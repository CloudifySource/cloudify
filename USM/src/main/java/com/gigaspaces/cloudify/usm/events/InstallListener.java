package com.gigaspaces.cloudify.usm.events;

public interface InstallListener extends PreInstallListener, PostInstallListener {
	EventResult onInstall();
	
}
