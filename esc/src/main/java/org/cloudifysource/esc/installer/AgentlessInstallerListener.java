package org.cloudifysource.esc.installer;

public interface AgentlessInstallerListener {
	
	void onInstallerEvent(String eventName, Object... args);
}
