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

/**
 * Identify a primitive wrapper class e.g. Integer, String
 * 
 * @author giladh, adaml
 *
 */
public class PrimitiveWrapper {
	private static final HashSet<Class<?>> PRIMITIVE_WRAPPER_TYPES = getWrapperTypes();
	
    public static boolean is(Class<?> cls) {
    	return (cls.isPrimitive() || PRIMITIVE_WRAPPER_TYPES.contains(cls));
    }
    
    private static HashSet<Class<?>> getWrapperTypes() {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(String.class);
        return ret;
    }	


    
	private PrimitiveWrapper() {} // non-instantiable

}
