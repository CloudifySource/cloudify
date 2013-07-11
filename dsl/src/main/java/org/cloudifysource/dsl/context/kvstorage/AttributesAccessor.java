/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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


/**
 * 
 * @author adaml
 *
 */
public interface AttributesAccessor {

	/**********
	 * Groovy implementation for setter.
	 * @param key element key.
	 * @param value element value/
	 * @return previous value.
	 */
	Object putAt(final Object key, final Object value);
	
	/**
	 * Sets property value.
	 * @param name
	 * 		property name.
	 * @param value
	 * 		property value.
	 */
	void setProperty(final String name, final Object value);
	
	/************
	 * Groovy getter.
	 * @param key element key.
	 * @return element value.
	 */
	Object getAt(final Object key);
	
	/**
	 * returns property value. 
	 * @param property
	 * 			property name.
	 * @return
	 * 			property value.
	 */
	Object getProperty(final String property);
	
	/********
	 * Groovy element remover.
	 * @param key element key.
	 * @return the element.
	 */
	Object remove(final String key);
	
	/*********
	 * Clears the attributes.
	 */
	void clear();
	
	/*********
	 * Groovy element accessor.
	 * @param key the element key.
	 * @return the element value.
	 */
	Object get(final String key);
	
	/**************
	 * check if attribute with specified key exists.
	 * @param key the element key.
	 * @return true if the an element with this key exists, false otherwise.
	 */
	boolean containsKey(final String key);
	
}
