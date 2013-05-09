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
package org.cloudifysource.security;

/**
 * Security related constants.
 * @author noak
 *
 */
public final class SecurityConstants {
	
	private SecurityConstants() {
		// private constructor to prevent initialization.
	}
	
	public static final String SECURITY_FILE_NAME = "spring-security.xml";
	public static final String KEYSTORE_FILE_NAME = "keystore";
	public static final String SPRING_SECURITY_CONFIG_FILE_ENV_VAR = "SPRING_SECURITY_CONFIG_FILE";
	public static final String KEYSTORE_FILE_ENV_VAR = "KEYSTORE_FILE";
	public static final String KEYSTORE_PASSWORD_ENV_VAR = "KEYSTORE_KEY";
	public static final String SPRING_ACTIVE_PROFILE_ENV_VAR = "SPRING_PROFILES_ACTIVE";
	public static final String SPRING_PROFILE_NON_SECURE = "nonsecure";
	public static final String SPRING_PROFILE_SECURE = "secure";

}
