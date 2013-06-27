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
package org.cloudifysource.dsl.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*******
 * Annotation indicating a POJO can be used in the Cloudify DSL.
 * @author barakme
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CloudifyDSLEntity {
	
	/**********
	 * The name used to identify the POJO in the DSL.
	 * @return The name used to identify the POJO in the DSL.
	 */
	String name();
	
	/***********
	 * If the POJO is internal, specifies the name of the parent node.  
	 */
	String parent() default "";
	
	/*********
	 * The class of the POJO.
	 */
	Class<?> clazz();
	
	/*********
	 * True if the POJO may be the root node of a DSL file, false otherwise.
	 */
	boolean allowRootNode();
	
	/*******
	 * True if the POJO may be an internal node of a DSL file, false otherwise.
	 * @return boolean, True if the POJO may be an internal node of a DSL file, false otherwise. 
	 */
	boolean allowInternalNode();
	
}
