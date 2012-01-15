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
package org.cloudifysource.dsl.context.kvstorage;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import org.cloudifysource.dsl.context.kvstorage.spaceentries.AbstractCloudifyAttribute;
import org.openspaces.core.GigaSpace;


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
	
	public Object remove(String key){
	    GigaSpace managementSpace = attributesFacade.getManagementSpace();
        AbstractCloudifyAttribute removeTemplate = prepareAttributeTemplate(key);
        AbstractCloudifyAttribute previousValue = managementSpace.take(removeTemplate);
        return previousValue != null? previousValue.getValue() : null;
	}
	
	public void clear(){
	    GigaSpace managementSpace = attributesFacade.getManagementSpace();
	    AbstractCloudifyAttribute clearTemplate = prepareAttributeTemplate(null);
	    managementSpace.clear(clearTemplate);
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