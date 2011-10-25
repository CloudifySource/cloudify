package com.gigaspaces.cloudify.usm.events;

public interface PostInstallListener extends USMEvent {

	EventResult onPostInstall();
}
