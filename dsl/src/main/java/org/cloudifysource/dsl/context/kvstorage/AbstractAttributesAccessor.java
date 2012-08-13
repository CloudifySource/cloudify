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
 * Base class for accessing attributes.
 * 
 * @author eitany
 * @since 2.0
 */
public abstract class AbstractAttributesAccessor extends GroovyObjectSupport {

	protected final AttributesFacade attributesFacade;
	protected final String applicationName;

	public AbstractAttributesAccessor(final AttributesFacade attributesFacade, final String applicationName) {
		this.attributesFacade = attributesFacade;
		this.applicationName = applicationName;
	}

	/**********
	 * Groovy implementation for setter.
	 * @param key element key.
	 * @param value element value/
	 * @return previous value.
	 */
	public Object putAt(final Object key, final Object value) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException("key must be a string");
		}

		return put((String) key, value);
	}

	@Override
	public void setProperty(final String name, final Object value) {
		try {
			super.setProperty(name, value);
		} catch (final MissingPropertyException e) {
			put(name, value);
		}
	}

	private Object put(final String key, final Object value) {
		final GigaSpace managementSpace = attributesFacade.getManagementSpace();
		final AbstractCloudifyAttribute attributeEntry = prepareAttributeTemplate(key);
		final AbstractCloudifyAttribute previousValue = managementSpace.take(attributeEntry);
		attributeEntry.setValue(value);
		managementSpace.write(attributeEntry);
		return previousValue != null ? previousValue.getValue() : null;
	}

	/************
	 * Groovy getter.
	 * @param key element key.
	 * @return element value.
	 */
	public Object getAt(final Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException("key must be a string");
		}

		return get((String) key);
	}

	@Override
	public Object getProperty(final String property) {
		try {
			return super.getProperty(property);
		} catch (final MissingPropertyException e) {
			return get(property);
		}
	}

	/********
	 * Groovy element remover.
	 * @param key element key.
	 * @return the element.
	 */
	public Object remove(final String key) {
		final GigaSpace managementSpace = attributesFacade.getManagementSpace();
		final AbstractCloudifyAttribute removeTemplate = prepareAttributeTemplate(key);
		final AbstractCloudifyAttribute previousValue = managementSpace.take(removeTemplate);
		return previousValue != null ? previousValue.getValue() : null;
	}

	/*********
	 * Clears the attributes.
	 */
	public void clear() {
		final GigaSpace managementSpace = attributesFacade.getManagementSpace();
		final AbstractCloudifyAttribute clearTemplate = prepareAttributeTemplate(null);
		managementSpace.clear(clearTemplate);
	}

	/*********
	 * Groovy element accessor.
	 * @param key the element key.
	 * @return the element value.
	 */
	public Object get(final String key) {
		final GigaSpace managementSpace = attributesFacade.getManagementSpace();
		final AbstractCloudifyAttribute propertyEntry = prepareAttributeTemplate(key);
		final AbstractCloudifyAttribute valueEntry = managementSpace.read(propertyEntry);
		return valueEntry != null ? valueEntry.getValue() : null;
	}

	/**************
	 * check if attribute with specified key exists.
	 * @param key the element key.
	 * @return true if the an element with this key exists, false otherwise.
	 */
	public boolean containsKey(final String key) {
		final GigaSpace managementSpace = attributesFacade.getManagementSpace();
		final AbstractCloudifyAttribute propertyEntry = prepareAttributeTemplate(key);
		return managementSpace.count(propertyEntry) > 0;
	}

	private AbstractCloudifyAttribute prepareAttributeTemplate(final String key) {
		final AbstractCloudifyAttribute propertyAttribute = prepareAttributeTemplate();
		propertyAttribute.setApplicationName(applicationName);
		propertyAttribute.setKey(key);
		return propertyAttribute;
	}

	/********
	 * Initialize a POJO with the specific type used with this class.
	 * @return the created POJO.
	 */
	protected abstract AbstractCloudifyAttribute prepareAttributeTemplate();

}