package org.cloudifysource.usm.events;

public interface LifecycleListener extends InitListener, InstallListener, StartListener, StopListener,
		ShutdownListener, ServiceActionListener {

}
