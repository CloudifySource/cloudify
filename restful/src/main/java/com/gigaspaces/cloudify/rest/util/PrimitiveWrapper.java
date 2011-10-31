package com.gigaspaces.cloudify.rest.util;

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
