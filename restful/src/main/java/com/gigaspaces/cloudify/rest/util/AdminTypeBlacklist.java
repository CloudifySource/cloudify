package com.gigaspaces.cloudify.rest.util;

import java.util.HashSet;

import org.openspaces.admin.Admin;

/**
 * Contains a blacklist of types that denotes a  
 * getter function as irrelevant to the REST API, e.g. Admin, Void
 * 
 * @author giladh
 *
 */
public class AdminTypeBlacklist {
      
    public static boolean in(Class<?> cls) {
    	if (cls == null) {
    		throw new RuntimeException("cls != null");
    	}
    	return ADMIN_TYPE_BLACKLIST.contains(cls);
    }
  
    private static HashSet<Class<?>> getBlacklistTypes() {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(void.class); // getter must return value
        ret.add(Void.class);
        ret.add(Admin.class);
        return ret;
    }	
    
	private static final HashSet<Class<?>> ADMIN_TYPE_BLACKLIST = getBlacklistTypes();
	
	private AdminTypeBlacklist() {} // non-instantiable
	
}
