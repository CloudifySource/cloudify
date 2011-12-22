package com.gigaspaces.cloudify.dsl.context.kvstorage;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import org.openspaces.core.GigaSpace;

import com.gigaspaces.cloudify.dsl.context.kvstorage.spaceentries.AbstractCloudifyAttribute;

/**
 * Base class for accessing attributes
 * @author eitany
 * @since 2.0
 */
public abstract class AbstractAttributesAccessor extends GroovyObjectSupport {

	protected final AttributesFacade attributesFacade;
	protected final String applicationName;

	public AbstractAttributesAccessor(AttributesFacade attributesFacade, String applicationName) {
		this.attributesFacade = attributesFacade;
		this.applicationName = applicationName; 
	}

	public Object putAt(Object key, Object value) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("key must be a string");
		
		return put((String) key, value);
	}
	
	@Override
	public void setProperty(String name, Object value) {
		try{
			super.setProperty(name, value);
		} catch(MissingPropertyException e){
			put(name, value);
		}
	}

	private Object put(String key, Object value) {
		GigaSpace managementSpace = attributesFacade.getManagementSpace();
		AbstractCloudifyAttribute attributeEntry = prepareAttributeTemplate(key);
		AbstractCloudifyAttribute previousValue = managementSpace.take(attributeEntry);
		attributeEntry.setValue(value);
		managementSpace.write(attributeEntry);
		return previousValue != null? previousValue.getValue() : null;
	}

	public Object getAt(Object key) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("key must be a string");
		
		return get((String) key);
	}
	
	@Override
	public Object getProperty(String property) {
		try{
			return super.getProperty(property);
		} catch(MissingPropertyException e){
			return get(property);
		}
	}

	public Object get(String key) {
		GigaSpace managementSpace = attributesFacade.getManagementSpace();
		AbstractCloudifyAttribute propertyEntry = prepareAttributeTemplate(key);
		AbstractCloudifyAttribute valueEntry = managementSpace.read(propertyEntry);
		return valueEntry != null? valueEntry.getValue() : null;
	}

	public boolean containsKey(String key) {
		GigaSpace managementSpace = attributesFacade.getManagementSpace();
		AbstractCloudifyAttribute propertyEntry = prepareAttributeTemplate(key);
		return managementSpace.count(propertyEntry) > 0;
	}

	private AbstractCloudifyAttribute prepareAttributeTemplate(String key) {
		AbstractCloudifyAttribute propertyAttribute = prepareAttributeTemplate();
		propertyAttribute.setApplicationName(applicationName);
		propertyAttribute.setKey(key);
		return propertyAttribute;
	}

	protected abstract AbstractCloudifyAttribute prepareAttributeTemplate();

}