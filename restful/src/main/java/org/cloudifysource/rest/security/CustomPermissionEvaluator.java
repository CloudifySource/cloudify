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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


public class CustomPermissionEvaluator implements PermissionEvaluator {
	
	private static final String PERMISSION_TO_DEPLOY = "deploy";
	private static final String PERMISSION_TO_VIEW = "view";
	private static final String AUTH_GROUPS_DELIMITER = ",";
	private static final String ROLE_CLOUDADMIN = "ROLE_CLOUDADMINS";
	private static final String ROLE_APPMANAGER = "ROLE_APPMANAGERS";
	private static final String ROLE_VIEWER = "ROLE_VIEWERS";
	
	private Logger logger = java.util.logging.Logger.getLogger(CustomPermissionEvaluator.class.getName());

	/**
	 * hasPermission
	 * @param authentication
	 * @param targetDomainObject
	 * @param permission
	 */
	@Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		
		boolean permissionGranted = false;
		String permissionName, authGroupsString;
		Collection<String> requestedAuthGroups, userAuthGroups;
		
    	if (authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
		if (permission != null && !(permission instanceof String)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, invalid permission object type: "
    				+ permission.getClass().getName());			
		}
		
		permissionName = (String)permission;
    	if (StringUtils.isBlank(permissionName)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, missing permission name");
    	}
    	
    	if (!permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW) && 
    			!permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
    		throw new AuthorizationServiceException("Unsupported permission name: " + permissionName
    				+ ". valid permission names are: " + PERMISSION_TO_VIEW + ", " + PERMISSION_TO_DEPLOY);
    	}
    	
    	if (targetDomainObject != null && !(targetDomainObject instanceof String)) {
    		throw new AuthorizationServiceException("Failed to verify permissions, invalid authorization groups object"
    				+ " type: " + targetDomainObject.getClass().getName());			
		}
    	
    	if (targetDomainObject == null) {
    		targetDomainObject = "";
    	}
    	authGroupsString = ((String)targetDomainObject).trim();

    	requestedAuthGroups = splitAndTrim(authGroupsString, AUTH_GROUPS_DELIMITER);
    	userAuthGroups = getUserAuthGroups();
    	
    	//check roles
    	boolean relevantRoleFound = false;
    	if (permissionName.equalsIgnoreCase(PERMISSION_TO_VIEW)) {
    		for (String userAuthGroup : userAuthGroups) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(userAuthGroup) ||
    					ROLE_APPMANAGER.equalsIgnoreCase(userAuthGroup) ||
    					ROLE_VIEWER.equalsIgnoreCase(userAuthGroup)) {
    				relevantRoleFound = true;
    				break;
    			}
    		}
    	} else if (permissionName.equalsIgnoreCase(PERMISSION_TO_DEPLOY)) {
    		for (String userAuthGroup : userAuthGroups) {
    			if (ROLE_CLOUDADMIN.equalsIgnoreCase(userAuthGroup) ||
    					ROLE_APPMANAGER.equalsIgnoreCase(userAuthGroup)) {
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
	 */
	@Override
	public boolean hasPermission(Authentication authentication,
			Serializable targetId, String targetType, Object permission) {
		logger.warning("Evaluating expression using hasPermission signature #2");

		return false;
	}
	
	public boolean hasPermissionToView(Collection<String> requestedAuthGroups) {
		
		if (requestedAuthGroups.size() == 0) {
			return true;
		}
		
    	return hasAnyAuthGroup(requestedAuthGroups);
    }
    
	public boolean hasPermissionToDeploy(Collection<String> requestedAuthGroups) {
		if (requestedAuthGroups.size() == 0) {
			return true;
		}
    	return hasAllAuthGroups(requestedAuthGroups);
    }
    
    private boolean hasAllAuthGroups(Collection<String> requestedAuthGroups) throws AccessDeniedException {
    	boolean isPermitted = false;
    	
    	Collection<String> userAuthGroups = getUserAuthGroups();
		if (userAuthGroups.containsAll(requestedAuthGroups)) {
			isPermitted = true;
			/*String msg = "Insufficient permissions. User " + authentication.getName() + " is only permitted to "
					+ "view and authorize groups: " + Arrays.toString(userAuthGroups.toArray(new String[0]));
			logger.log(Level.INFO, msg);
			throw new AccessDeniedException(msg);*/
		}
		
		return isPermitted;
    }
    
    private boolean hasAnyAuthGroup(Collection<String> requestedAuthGroups) throws AccessDeniedException {
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
    
    public Collection<String> getUserAuthGroups() throws AccessDeniedException {
    	Set<String> userAuthGroups = new HashSet<String>();
    	
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	if (authentication instanceof AnonymousAuthenticationToken) {
			throw new AccessDeniedException("Anonymous user is not supported");
    	}
		
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			userAuthGroups.add(authority.getAuthority());
		}
		
		return userAuthGroups;
    }
    
    private Collection<String> splitAndTrim(String csvString, String delimiter) {
    	Collection<String> values = new HashSet<String>();
		StringTokenizer tokenizer = new StringTokenizer(csvString, delimiter);
		while (tokenizer.hasMoreTokens()) {
			values.add(tokenizer.nextToken().trim());
		}
		
		return values;
    }

}