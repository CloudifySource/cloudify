/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A custom PermissionEvaluator which performs permission decisions based on roles assignments and 
 * authorization groups membership.
 *
 * @author noak
 * @since 3.2.0
 */
public class CustomPermissionEvaluator implements PermissionEvaluator {
	
	// TODO [noak] : use authority groups and not roles
	
	private static final String PERMISSION_TO_DEPLOY = "deploy";
	private static final String PERMISSION_TO_VIEW = "view";
	private static final String AUTH_GROUPS_DELIMITER = ",";
	private static final String ROLE_CLOUDADMIN = "ROLE_CLOUDADMINS";
	private static final String ROLE_APPMANAGER = "ROLE_APPMANAGERS";
	private static final String ROLE_VIEWER = "ROLE_VIEWERS";
	private static final String SPRING_SECURITY_PROFILE = 
			System.getenv(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR);
	
	private Logger logger = java.util.logging.Logger.getLogger(CustomPermissionEvaluator.class.getName());

	/**
	 * Checks if the current user should be granted the requested permission on the target object.
	 * @param authentication The authentication object of the current user
	 * @param targetDomainObject The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	@Override
    public boolean hasPermission(final Authentication authentication, final Object targetDomainObject, 
    		final Object permission) {
		
		return hasPermission(new CloudifyAuthorizationDetails(authentication), targetDomainObject, permission);
	}
	
	/**
	 * Checks if the current user should be granted the requested permission on the target object.
	 * @param authDetails The CloudifyAuthorizationDetails object of the current user
	 * @param targetDomainObject The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
    public boolean hasPermission(final CloudifyAuthorizationDetails authDetails, final Object targetDomainObject, 
    		final Object permission) {
		
		if (StringUtils.isBlank(SPRING_SECURITY_PROFILE) 
				|| SPRING_SECURITY_PROFILE.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_NON_SECURE)) {
			//security is off
			return true;
		}
		
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Starting \"hasPermission\" for user: " + authDetails.getUsername());
			logger.finest("with roles: " + collectionToDelimitedString(authDetails.getRoles(), ","));
			logger.finest("and with authGroups: " + collectionToDelimitedString(authDetails.getAuthGroups(), ","));
			logger.finest("requested permission: " + permission.toString());
			logger.finest("on target authGroups: " + targetDomainObject.toString());
		}		
		
		boolean permissionGranted = false;
		String permissionName, targetAuthGroups;
		
		//tested already in the ctor of CloudifyAuthorizationDetails
    	/*if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
    	
    	if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
    		throw new AccessDeniedException("Authentication object type not supported. "
    				+ "Verify your Spring configuration is valid.");
    	}*/
		
		if (permission != null && !(permission instanceof String)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, invalid permission object type: "
    				+ permission.getClass().getName());			
		}
		
		permissionName = (String) permission;
    	if (StringUtils.isBlank(permissionName)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, missing permission name");
    	}
    	
    	if (!permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW) 
    			&& !permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
    		throw new AuthorizationServiceException("Unsupported permission name: " + permissionName
    				+ ". valid permission names are: " + PERMISSION_TO_VIEW + ", " + PERMISSION_TO_DEPLOY);
    	}
    	
    	if (targetDomainObject != null && !(targetDomainObject instanceof String)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, invalid authorization groups object"
    				+ " type: " + targetDomainObject.getClass().getName());			
		}
    	
    	if (targetDomainObject == null) {
    		targetAuthGroups = "";
    	} else {
    		targetAuthGroups = ((String) targetDomainObject).trim();	
    	}
    	
		if (hasRequiredRoles(authDetails, permissionName) 
				&& hasAuthGroupAccess(authDetails, targetAuthGroups, permissionName)) {
			permissionGranted = true;
		}
		
     	return permissionGranted;
    }
	

	/**
	 * Checks if the current user should be granted the requested permission on the target object.
	 * This signature is currently not implemented.
	 * @param authentication The authentication object of the current user
	 * @param targetId The A unique identifier of the target object the user is attempting to access
	 * @param targetType The type of the target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	@Override
	public boolean hasPermission(final Authentication authentication, final Serializable targetId, 
			final String targetType, final Object permission) {
		logger.warning("Evaluating expression using hasPermission unimplemented signature");

		if (SecuredObjectTypes.AUTHORIZATION_GROUP.toString().equalsIgnoreCase(targetType)) {
			return hasPermission(authentication, targetId, permission);
		} else {
			throw new IllegalArgumentException("This object type cannot be secured: " + targetType);
		}
	}
	
	/**
	 * Verifies the current user has the requested permission on the target object.
	 * @param targetDomainObject The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 */
	/*public void verifyPermission(final Object targetDomainObject, final Object permission) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		verifyPermission(authentication, targetDomainObject, permission);
	}*/
	
	/**
	 * Verifies the current user has the requested permission on the target object.
	 * @param targetDomainObject The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @param authentication The object representing the current user
	 */
	/*public void verifyPermission(final Authentication authentication, final Object targetDomainObject,
			final Object permission) {
		verifyPermission(new CloudifyAuthorizationDetails(authentication), targetDomainObject, permission);
	}*/
	
	/**
	 * Verifies the current user has the requested permission on the target object.
	 * @param authDetails The CloudifyAuthorizationDetails object representing the current user authorization details
	 * @param targetDomainObject The target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 */
	public void verifyPermission(final CloudifyAuthorizationDetails authDetails, final Object targetDomainObject,
			final Object permission) {
		if (!hasPermission(authDetails, targetDomainObject, permission)) {
			throw new AccessDeniedException("User " + authDetails.getUsername() + " is not permitted to "
					+ "access the target objects");
		}
	}
	
	
	/**
	 * Checks if the logged in user is allowed to access the target object, according to its roles.
	 * @param authDetails The CloudifyAuthorizationDetails object of the logged in user.
	 * @param permissionName permission requested (view, deploy, etc.)
	 * @return true - access allowed, false - access denied.
	 */
	private boolean hasRequiredRoles(final CloudifyAuthorizationDetails authDetails, final String permissionName) {
		
		boolean relevantRoleFound = false;
		
		Collection<String> userRoles = authDetails.getRoles();
		
		//TODO [noak] : This logic should be configurable
		
    	if (permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW)) {
    		for (String role : userRoles) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(role) 
    					|| ROLE_APPMANAGER.equalsIgnoreCase(role) 
    					|| ROLE_VIEWER.equalsIgnoreCase(role)) {
    				relevantRoleFound = true;
    				break;
    			}
    		}
    	} else if (permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
    		for (String role : userRoles) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(role) 
    					|| ROLE_APPMANAGER.equalsIgnoreCase(role)) {
    				relevantRoleFound = true;
    				break;
    			}
    		}
    	}
    	
    	if (!relevantRoleFound) {
    		logger.log(Level.WARNING, "User " + authDetails.getUsername() + " is missing the required roles, access is "
					+ "denied.");
    	}
    	
    	return relevantRoleFound;
	}
	
	/**
	 * Checks if the logged in user is allowed to access the target object, according to its authorization groups.
	 * @param authDetails CloudifyAuthorizationDetails object of the logged in user
	 * @param targetAuthGroupsStr Comma delimited string of the target object's authorization groups.
	 * @param permissionName permission requested (view, deploy, etc.)
	 * @return true - access allowed, false - access denied.
	 */
	private boolean hasAuthGroupAccess(final CloudifyAuthorizationDetails authDetails, 
			final String targetAuthGroupsStr, final String permissionName) {
		
		boolean permissionGranted = false;
		Collection<String> userAuthGroups = authDetails.getAuthGroups();
    	
    	/*if (authentication instanceof CustomAuthenticationToken) {
    		userAuthGroups = ((CustomAuthenticationToken) authentication).getAuthGroups();
    	} else {
    		//auth groups don't exist in this configuration, so use roles as authGroups.
    		userAuthGroups = getUserRoles();
    	}*/
    	
    	Collection<String> targetAuthGroups = 
    			org.cloudifysource.esc.util.StringUtils.splitAndTrimString(targetAuthGroupsStr, AUTH_GROUPS_DELIMITER);
		
		if (permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW)) {
			if (hasPermissionToView(targetAuthGroups)) {
				permissionGranted = true;
				logger.log(Level.INFO, "View permission granted for user " + authDetails.getUsername());
			} else {
				logger.log(Level.WARNING, "Insufficient permissions. User " + authDetails.getUsername() + " is only "
						+ "permitted to view groups: " + Arrays.toString(userAuthGroups.toArray(new String[0])));
			}
		} else if (permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
			if (hasPermissionToDeploy(authDetails, targetAuthGroups)) {
				permissionGranted = true;
				logger.log(Level.INFO, "Deploy permission granted for user " + authDetails.getUsername());
			} else {
				logger.log(Level.WARNING, "Insufficient permissions. User " + authDetails.getUsername() + " is only "
						+ "permitted to deploy for groups: " + Arrays.toString(userAuthGroups.toArray(new String[0])));
			}
		}
		
		return permissionGranted;
	}
	
	
	/**
	 * Verifies the current user has the requested permission on the target object.
	 * @param targetId The A unique identifier of the target object the user is attempting to access
	 * @param targetType The type of the target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 */
	/*public void verifyPermission(final Serializable targetId, final String targetType, final Object permission) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!hasPermission(authentication, targetId, targetType, permission)) {
			throw new AccessDeniedException("User " + authentication.getName() + " is not permitted to " 
					+ "access the target objects");
		}
	}*/
	
	
	/**
	 * Checks if the current user is allowed to view the an object that has the specified authorization groups.
	 * If the user has *any* of the target object's authorization groups - permission to view it is granted.
	 * @param requestedAuthGroups The authorization groups of the target object
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	private boolean hasPermissionToView(final Collection<String> requestedAuthGroups) {
		
		if (requestedAuthGroups.isEmpty()) {
			return true;
		}
		
    	return hasAnyAuthGroup(requestedAuthGroups);
    }
	
    
	/**
	 * Checks if the current user is allowed to view the an object that has the specified authorization groups.
	 * If the user has *any* the authorization groups of the object - permission to view it is granted.
	 * @param authDetails The CloudifyAuthorizationDetails object of the user who requests permission
	 * @param requestedAuthGroups The authorization groups of the target object
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	private boolean hasPermissionToDeploy(final CloudifyAuthorizationDetails authDetails, 
			final Collection<String> requestedAuthGroups) {
		
		if (requestedAuthGroups.isEmpty()) {
			return true;
		}
		
		//if the current user has at any of the requested auth groups - deploy is permitted.
		return hasAnyAuthGroup(authDetails, requestedAuthGroups);
    	//return hasAllAuthGroups(requestedAuthGroups);
    }
    
    private boolean hasAllAuthGroups(final Collection<String> requestedAuthGroups) {
    	boolean isPermitted = false;
    	
    	Collection<String> userAuthGroups = getUserAuthGroups();
		if (userAuthGroups.containsAll(requestedAuthGroups)) {
			isPermitted = true;
		}
		
		return isPermitted;
    }
    
    private boolean hasAnyAuthGroup(final Collection<String> requestedAuthGroups) {
    	boolean isPermitted = false;
    	
    	Collection<String> userAuthGroups = getUserAuthGroups();
    	for (String requestedAuthGroup : requestedAuthGroups) {
    		/*if (userAuthGroups.contains(requestedAuthGroup)) {
    			isPermitted = true;
    			break;
    		}*/
    		for (String userAuthGroup : userAuthGroups) {
    			if (requestedAuthGroup.equalsIgnoreCase(userAuthGroup)) {
    				isPermitted = true;
    				break;
    			}
    		}
    	}
    	
		return isPermitted;
    }
    
    private boolean hasAnyAuthGroup(final CloudifyAuthorizationDetails authDetails, 
    		final Collection<String> requestedAuthGroups) {
    	
    	boolean isPermitted = false;
    	
    	Collection<String> userAuthGroups = authDetails.getAuthGroups();
    	for (String requestedAuthGroup : requestedAuthGroups) {
    		/*if (userAuthGroups.contains(requestedAuthGroup)) {
    			isPermitted = true;
    			break;
    		}*/
    		for (String userAuthGroup : userAuthGroups) {
    			if (requestedAuthGroup.equalsIgnoreCase(userAuthGroup)) {
    				isPermitted = true;
    				break;
    			}
    		}
    	}
    	
		return isPermitted;
    }
    
    /**
     * Returns the names of the roles (authorities) the user is granted.
     * @param authentication The authentication object of the current user
     * @return A Collection of roles (authorities) the user is granted.
     */
    private Collection<String> getUserRoles(final Authentication authentication) {
    	Set<String> userRoles = new HashSet<String>();
		
    	if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
    	if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
    		throw new AccessDeniedException("Authentication object type not supported. "
    				+ "Verify your Spring configuration is valid.");
    	}
		
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			userRoles.add(authority.getAuthority());
		}
		
		return userRoles;
    }
    
    /**
     * Returns the names of the roles (authorities) the user is granted.
     * @return A Collection of roles (authorities) the user is granted.
     */
    private Collection<String> getUserRoles() {
    	return getUserRoles(SecurityContextHolder.getContext().getAuthentication());
    }
    
    /**
     * Returns the names of the roles (authorities) the user is granted.
     * @return A String array of roles (authorities) names
     */
    public String getUserRolesString() {
    	Collection<String> userRoles = getUserRoles();
    	return collectionToDelimitedString(userRoles, ",");
    }
    
    /**
     * Returns the names of the roles (authorities) the user is granted.
     * @param authentication The authentication object of the current user
     * @return A String array of roles (authorities) names
     */
    public String getUserRolesString(final Authentication authentication) {
    	
    	Collection<String> userRoles = getUserRoles(authentication);
    	return collectionToDelimitedString(userRoles, ",");
    }
    
    /**
     * Returns the names of the authorization groups the user belongs to.
     *  @param authentication The authentication object of the current user
     * @return A String array of authorization groups names
     */
    public String getUserAuthGroupsString(final Authentication authentication) {
    	Collection<String> userAuthGroups = getUserAuthGroups(authentication);
    	return collectionToDelimitedString(userAuthGroups, ",");
    }
    
    /**
     * Returns the names of the authorization groups the user belongs to.
     * @return A String array of authorization groups names
     */
    public String getUserAuthGroupsString() {
    	Collection<String> userAuthGroups = getUserAuthGroups();
    	return collectionToDelimitedString(userAuthGroups, ",");
    }
    
    /**
     * Returns the names of the authorization groups the user belongs to.
     * @return A Collection of authorization groups the user belongs to.
     */
    private Collection<String> getUserAuthGroups() {
    	return getUserAuthGroups(SecurityContextHolder.getContext().getAuthentication());
    }
    
    /**
     * Returns the names of the authorization groups the user belongs to.
     * @param authentication The authentication object of the current user
     * @return A Collection of authorization groups names
     */
    private Collection<String> getUserAuthGroups(final Authentication authentication) {

    	Collection<String> userAuthGroups;
    	
    	if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
    	if (authentication instanceof CustomAuthenticationToken) {
    		userAuthGroups = ((CustomAuthenticationToken) authentication).getAuthGroups();
    	} else {
    		//auth groups don't exist in this configuration, so use roles as authGroups.
    		userAuthGroups = getUserRoles();    		
    	}

		
		return userAuthGroups;
    }
    
    
    private static String collectionToDelimitedString(final Collection<String> collection, final String delimiter) {
    	String delimitedString;
    	StringBuilder builder = new StringBuilder();
    	
    	for (String item : collection) {
    	    builder.append(item);
    	    builder.append(delimiter);
    	}

    	delimitedString = builder.toString();
    	if (delimitedString.endsWith(delimiter)) {
    		delimitedString = delimitedString.substring(0, delimitedString.length() - delimiter.length());
    	}
    	
    	return delimitedString;
    }

}