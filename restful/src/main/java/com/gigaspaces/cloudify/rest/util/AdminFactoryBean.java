package com.gigaspaces.cloudify.rest.util;

import com.gigaspaces.cloudify.rest.SecurityPropagation;
import com.gigaspaces.security.directory.UserDetails;
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
    AdminFactory adminFactory = new AdminFactory();
    Admin admin;
    SecurityPropagation securityPropagation;

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
        if (SecurityPropagation.CLUSTER.equals(securityPropagation)) {
            final org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) SecurityContextHolder
                    .getContext().getAuthentication().getPrincipal();
            adminFactory.userDetails(userDetails.getUsername(),userDetails.getPassword());
        }
        admin = adminFactory.createAdmin();
    }

    public void setLocators(String... locators) {
        for (String locator : locators) {
            adminFactory.addLocator(locator);
        }
    }

    public void setGroups(String... groups) {
        for (String group : groups) {
            adminFactory.addGroup(group);
        }
    }

    public void setDiscoverUnmanagedSpace(boolean discoverUnmanagedSpace) {
        if (discoverUnmanagedSpace) {
            adminFactory.discoverUnmanagedSpaces();
        }
    }

    public void setUserDetails(UserDetails userDetails) {
        adminFactory.userDetails(userDetails);
    }

    public void setUseGsLogging(boolean useGsLogging) {
        adminFactory.useGsLogging(useGsLogging);
    }

    public void setSecurityPropagation(SecurityPropagation securityPropagation) {
        this.securityPropagation = securityPropagation;
    }
}
