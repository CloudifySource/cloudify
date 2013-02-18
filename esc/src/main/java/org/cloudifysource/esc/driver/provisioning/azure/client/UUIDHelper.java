/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.util.UUID;

/**
 * 
 * @author elip
 *
 */
public final class UUIDHelper {
	
	private UUIDHelper() {
		
	}
	
	/**
	 * 
	 * @param length .
	 * @return .
	 */
	public static String generateRandomUUID(final int length) {
        UUID uuid = UUID.randomUUID();
        char[] uuidChars = uuid.toString().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(uuidChars[i]);
        }                           
        return builder.toString();
	}

}
