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
