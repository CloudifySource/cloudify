package com.gigaspaces.cloudify.dsl.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

/**
 * A dsl script reader for services
 */
public abstract class ServiceDslScript extends BaseDslScript {

	private static final Object EXTEND_PROPERTY_NAME = "extend";
	private int propertyCounter;
	
	@Override
	protected void beforeHandleInvokeMethod(String name, Object arg) {
		super.beforeHandleInvokeMethod(name, arg);
		
		if (name.equals("service"))
			propertyCounter = 0;
		else
			propertyCounter++;
	}
	
	@Override
	protected boolean handleSpecialProperty(String name, Object arg)
			throws DSLException {
		
		if (super.handleSpecialProperty(name, arg))
			return true;				
		
		if (name.equals(EXTEND_PROPERTY_NAME)){
			if (propertyCounter > 1)
				throw new DSLException(EXTEND_PROPERTY_NAME + " must be first inside the service block");
			if (arg != null && arg.getClass().isArray())
			{
				Object[] arr = (Object[]) arg;
				if (arr.length != 1)
					throw new DSLException(EXTEND_PROPERTY_NAME + " property must be a single string");
				arg = ((Object[])arg)[0];
			}
			if (!(arg instanceof String))
				throw new DSLException(EXTEND_PROPERTY_NAME + " property must be a string");
			if (!(this.activeObject instanceof Service))
				throw new DSLException(EXTEND_PROPERTY_NAME + " property can only be used on a service");
			String extendServicePath = (String) arg;
			try {
				Service baseService = ServiceReader.readService(new File(extendServicePath));
				BeanUtils.copyProperties(this.activeObject, baseService);
				return true;
			} catch (IOException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			} catch (PackagingException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			} catch (IllegalAccessException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			} catch (InvocationTargetException e) {
				throw new DSLException("Failed to parse extended service: " + extendServicePath, e);
			}
		}
		return false;
	}
	
}
