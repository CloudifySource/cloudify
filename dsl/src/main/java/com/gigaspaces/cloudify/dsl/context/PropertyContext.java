package com.gigaspaces.cloudify.dsl.context;

/**
 * Specifies property context
 * @author eitany
 * @since 2.0
 */
public enum PropertyContext {
	/**
	 * This property can be accessed in the application (via all services).
	 */
	APPLICATION,
	/**
	 * This property can be accessed in this service (via all instances).  
	 */
	SERVICE,
	/**
	 * This property can be accessed in this service instance only.
	 */
	INSTANCE
}
