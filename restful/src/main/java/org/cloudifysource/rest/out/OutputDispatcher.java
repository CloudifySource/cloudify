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
package org.cloudifysource.rest.out;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudifysource.rest.command.CommandManager;

public class OutputDispatcher {

	private OutputDispatcher(){}
	
	public static Map<String, Object> outputResultObjectToMap(CommandManager manager, String contextPath, String hostContext){
		OutputUtils.setHostAddress(contextPath);
		OutputUtils.setHostContext(hostContext);
		Map<String, Object> outputMap = new HashMap<String, Object>();
		Object object = manager.getFinalCommand().getCommandObject();
		if (OutputUtils.isNull(object)){
			outputMap.put(manager.getFinalCommandName(), OutputUtils.NULL_OBJECT_DENOTER);
			return outputMap;
		}
		String nextCommandURL;
		Class<?> aClass = object.getClass();
		if (aClass.isArray()){
		    nextCommandURL = OutputUtils.getNextCommandUrl(manager.getCommandURL(), manager.getFinalCommandName(), true);
			OutputUtils.outputArrayToMap(object, outputMap, nextCommandURL);
		}else if (Map.class.isAssignableFrom(aClass)) {
		    nextCommandURL = OutputUtils.getNextCommandUrl(manager.getCommandURL(), manager.getFinalCommandName(), true);
			OutputUtils.outputMapToMap(object, outputMap, nextCommandURL);
		}else if (List.class.isAssignableFrom(aClass)) {
		    nextCommandURL = OutputUtils.getNextCommandUrl(manager.getCommandURL(), manager.getFinalCommandName(), true);
			OutputUtils.outputListToMap(object, outputMap, nextCommandURL);
		}else{
			OutputUtils.outputObjectToMap(manager, outputMap);
		}
		return new TreeMap<String, Object>(outputMap);
	}
	
}
