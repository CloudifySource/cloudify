package com.gigaspaces.cloudify.usm.events;

public interface PreInstallListener extends USMEvent {

	EventResult onPreInstall();
}
