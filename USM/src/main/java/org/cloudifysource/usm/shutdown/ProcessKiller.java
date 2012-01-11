package org.cloudifysource.usm.shutdown;


import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.USMException;

public interface ProcessKiller extends USMComponent {

	void killProcess(long pid) throws USMException;
}
