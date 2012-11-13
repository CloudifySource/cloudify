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
import org.cloudifysource.esc.util.Utils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A custom PermissionEvaluator which... 
 *
 * @author noak
 * @since 3.2.0
 */
public class CustomPermissionEvaluator implements PermissionEvaluator {
	
	private static final String PERMISSION_TO_DEPLOY = "deploy";
	private static final String PERMISSION_TO_VIEW = "view";
	private static final String AUTH_GROUPS_DELIMITER = ",";
	private static final String ROLE_CLOUDADMIN = "ROLE_CLOUDADMINS";
	private static final String ROLE_APPMANAGER = "ROLE_APPMANAGERS";
	private static final String ROLE_VIEWER = "ROLE_VIEWERS";
	private static final String IS_SPRING_SECURED = System.getenv(CloudifyConstants.SPRING_BEANS_PROFILE_ENV_VAR);
	
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
		
		boolean permissionGranted = false;
		String permissionName, authGroupsString;
		Collection<String> requestedAuthGroups, userAuthGroups;
		
    	if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
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
    		authGroupsString = "";
    	} else {
    		authGroupsString = ((String) targetDomainObject).trim();	
    	}
    	
    	requestedAuthGroups = Utils.splitAndTrimString(authGroupsString, AUTH_GROUPS_DELIMITER);
    	userAuthGroups = getUserAuthGroups();
    	
    	//check roles
    	//TODO : This should be configurable
    	boolean relevantRoleFound = false;
    	if (permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW)) {
    		for (String userAuthGroup : userAuthGroups) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(userAuthGroup) 
    					|| ROLE_APPMANAGER.equalsIgnoreCase(userAuthGroup) 
    					|| ROLE_VIEWER.equalsIgnoreCase(userAuthGroup)) {
    				relevantRoleFound = true;
    				break;
    			}
    		}
    	} else if (permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
    		for (String userAuthGroup : userAuthGroups) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(userAuthGroup) 
    					|| ROLE_APPMANAGER.equalsIgnoreCase(userAuthGroup)) {
    				relevantRoleFound = true;
    				break;
    			}
    		}
    	}
    	
    	//check auth-group permissions
		if (relevantRoleFound) {
			if (permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW)) {
				if (hasPermissionToView(requestedAuthGroups)) {
					permissionGranted = true;
					logger.log(Level.INFO, "View permission granted for user " + authentication.getName());
				} else {
					logger.log(Level.INFO, "Insufficient permissions. User " + authentication.getName() + " is only "
							+ "permitted to view groups: " + Arrays.toString(userAuthGroups.toArray(new String[0])));
				}
			} else if (permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
				if (hasPermissionToDeploy(requestedAuthGroups)) {
					permissionGranted = true;
					logger.log(Level.INFO, "Deploy permission granted for user " + authentication.getName());
				} else {
					logger.log(Level.INFO, "Insufficient permissions. User " + authentication.getName() + " is only "
							+ "permitted to deploy for groups: " 
							+ Arrays.toString(userAuthGroups.toArray(new String[0])));
				}
			}
		} else {
			logger.log(Level.INFO, "User " + authentication.getName() + "is missing the required roles, access is "
					+ "denied.");
		}
    	
		
     	return permissionGranted;
    }

	/**
	 * Another hasPermission signature. We will not implement this.
	 * @param authentication
	 */
	/**
	 * Checks if the current user should be granted the requested permission on the target object.
	 * This signature is currently not implemented.
	 * @param authentication The authentication object of the current user
	 * @param targetId The A unique identifier of the target object the user is attempting to access
	 * @param targetType The type of the target object the user is attempting to access
	 * @param permission The permission requested on the target object (e.g. view, deploy)
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	public boolean hasPermission(final Authentication authentication, final Serializable targetId, 
			final String targetType, final Object permission) {
		logger.warning("Evaluating expression using hasPermission unimplemented signature");

		return false;
	}
	
	/**
	 * Checks if the current user is allowed to view the an object that has the specified authorization groups.
	 * If the user has *any* of the target object's authorization groups - permission to view it is granted.
	 * @param requestedAuthGroups The authorization groups of the target object
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	private boolean hasPermissionToView(final Collection<String> requestedAuthGroups) {
		
		if (requestedAuthGroups.size() == 0) {
			return true;
		}
		
    	return hasAnyAuthGroup(requestedAuthGroups);
    }
    
	/**
	 * Checks if the current user is allowed to view the an object that has the specified authorization groups.
	 * If the user has *all* the authorization groups of the object - permission to view it is granted.
	 * @param requestedAuthGroups The authorization groups of the target object
	 * @return boolean value - true if permission is granted, false otherwise.
	 */
	private boolean hasPermissionToDeploy(final Collection<String> requestedAuthGroups) {
		if (requestedAuthGroups.size() == 0) {
			return true;
		}
		
    	return hasAllAuthGroups(requestedAuthGroups);
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
     * Returns the names of the authorities the user is granted.
     * @return A Collection of authorities the user is granted.
     */
    private Collection<String> getUserAuthGroups() {
    	Set<String> userAuthGroups = new HashSet<String>();
    	
    	if (StringUtils.isNotBlank(IS_SPRING_SECURED) && IS_SPRING_SECURED.equalsIgnoreCase("true")) {
    		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        	if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
    			throw new AccessDeniedException("Anonymous user is not supported");
        	}
    		
    		for (GrantedAuthority authority : authentication.getAuthorities()) {
    			userAuthGroups.add(authority.getAuthority());
    		}
    	}
    	
    	
		
		return userAuthGroups;
    }
    
    /**
     * Returns the names of the authorities the user is granted.
     * @return A String array of authorities names
     */
    public String getUserAuthGroupsString() {
    	Collection<String> userAuthGroups = getUserAuthGroups();		
		return Arrays.toString(userAuthGroups.toArray(new String[0]));
    }

}