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
package org.cloudifysource.rest.util;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gigaspaces.security.directory.UserDetails;

import net.jini.core.discovery.LookupLocator;

import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.rest.SecurityPropagation;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author uri
 */
public class AdminFactoryBean implements FactoryBean, InitializingBean, DisposableBean {
    private AdminFactory adminFactory = new AdminFactory();
    private Admin admin;
    private SecurityPropagation securityPropagation;

    private static final Logger logger = Logger.getLogger(AdminFactoryBean.class.getName());
    
	public Object getObject() throws Exception {
        return admin;
    }

	public Class<?> getObjectType() {
        return Admin.class;
    }

	public boolean isSingleton() {
        return true;
    }
	
	public void destroy() throws Exception {
        admin.close();
    }

	public void afterPropertiesSet() throws Exception {
        if (securityPropagation == SecurityPropagation.CLUSTER) {
            final org.springframework.security.core.userdetails.UserDetails userDetails = 
            		(org.springframework.security.core.userdetails.UserDetails) SecurityContextHolder
                    .getContext().getAuthentication().getPrincipal();
            adminFactory.userDetails(userDetails.getUsername(), userDetails.getPassword());
        }
        admin = adminFactory.createAdmin();
        if (logger.isLoggable(Level.INFO)) {
	        LookupLocator[] locators = admin.getLocators();
	        String[] locatorStrings = new String[locators.length];
	        for (int i = 0; i < locators.length; i++) {
	        	locatorStrings[i] = IPUtils.getSafeIpAddress(locators[i].getHost()) + ":" + locators[i].getPort();
	        }
	        logger.info("Admin using lookup locators=" + Arrays.toString(locatorStrings)
	        		+ " groups=" + Arrays.toString(admin.getGroups()));
        }
    }

    /**
     * 
     * @param locators .
     */
    public void setLocators(final String... locators) {
        for (String locator : locators) {
            adminFactory.addLocator(locator);
        }
        if (logger.isLoggable(Level.INFO)) {
        	logger.info("Configured lookup locators=" + Arrays.toString(locators));
        }
    }

    /**
     * 
     * @param groups .
     */
    public void setGroups(final String... groups) {
        for (String group : groups) {
            adminFactory.addGroup(group);
        }
        if (logger.isLoggable(Level.INFO)) {
        	logger.info("Configured lookup groups=" + Arrays.toString(groups));
        }
    }

    /**
     * 
     * @param discoverUnmanagedSpace .
     */
    public void setDiscoverUnmanagedSpace(final boolean discoverUnmanagedSpace) {
        if (discoverUnmanagedSpace) {
            adminFactory.discoverUnmanagedSpaces();
        }
    }

    /**
     * 
     * @param userDetails .
     */
    public void setUserDetails(final UserDetails userDetails) {
        adminFactory.userDetails(userDetails);
    }

    /**
     * 
     * @param useGsLogging .
     */
    public void setUseGsLogging(final boolean useGsLogging) {
        adminFactory.useGsLogging(useGsLogging);
    }

    public void setSecurityPropagation(final SecurityPropagation securityPropagation) {
        this.securityPropagation = securityPropagation;
    }
}
