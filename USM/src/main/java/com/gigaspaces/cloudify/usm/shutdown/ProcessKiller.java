package com.gigaspaces.cloudify.usm.shutdown;


import com.gigaspaces.cloudify.usm.USMComponent;
import com.gigaspaces.cloudify.usm.USMException;

public interface ProcessKiller extends USMComponent {

	void killProcess(long pid) throws USMException;
}
