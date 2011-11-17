package com.gigaspaces.cloudify.dsl.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CloudifyDSLEntity {
	
	String name();
	String parent() default "";	
	Class<?> clazz();
	boolean allowRootNode();
	boolean allowInternalNode();
	
}
