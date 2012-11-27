package org.cloudifysource.rest.security;

/**
 * Enumeration of the types of objects that can be secured by the permission evaluator.
 * @author noak
 * @since 2.3.0
 *
 */
public enum SecuredObjectTypes {
	
	/**
	 * Authorization groups.
	 */
	AUTHORIZATION_GROUP, 
	
	/**
	 * Machines.
	 */
	MACHINE;
}
