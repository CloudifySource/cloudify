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
package org.cloudifysource.rest.security;

/**
 * Enumeration of the types of objects that can be secured by the permission evaluator.
 * @author noak
 * @since 2.3.0
 *
 */
public enum SecuredObjectTypes {
	
	/**
	 * Authorization groups.
	 */
	AUTHORIZATION_GROUP, 
	
	/**
	 * Machines.
	 */
	MACHINE
}
