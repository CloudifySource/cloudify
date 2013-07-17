package org.cloudifysource.dsl.utils;

import java.lang.reflect.Method;

public class ReflectionUtils {

	public static String getHomeDir() {
		final String gsEnvClassName = "org.cloudifysource.utilitydomain.openspaces.OpenspacesDomainUtils";
		try {
			final Object envObject = Class.forName(gsEnvClassName).newInstance();
			final Method homeDirMethod = envObject.getClass().getMethod("getHomeDirectory"); 
			return (String) homeDirMethod.invoke(envObject, (Object[]) null);
		} catch (Exception e) {
			//Failed since openspaces is not in classpath.
			//This is expected to happen.
			return "";
		}
	}
}
