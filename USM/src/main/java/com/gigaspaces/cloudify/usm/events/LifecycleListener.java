package com.gigaspaces.cloudify.usm.events;

public interface LifecycleListener extends InitListener, InstallListener, StartListener, StopListener,
		ShutdownListener, ServiceActionListener {

}
