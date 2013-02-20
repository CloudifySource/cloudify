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
package org.openspaces.usm;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class StringHandler extends StreamHandler {

	private StringBuilder sb = new StringBuilder();
	
	@Override
	public void publish(LogRecord record) {
		sb.append(record.getMessage()).append(System.getProperty("line.separator"));
		flush();
	}
	
	public String getLoggedMessages(){
		return sb.toString();
	}
}
