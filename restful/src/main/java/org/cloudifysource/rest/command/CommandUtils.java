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
package org.cloudifysource.rest.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.cloudifysource.rest.out.OutputUtils;


public class CommandUtils {
	
	private static Logger logger = Logger.getLogger(CommandUtils.class.getName());
	
	public static Object getObjectByCommand(String command, Object someObject) throws RuntimeException{
		Method commandMethod = getGetterMethodFromObject(command, someObject.getClass());
		return OutputUtils.safeInvoke(commandMethod, someObject);
	}

	public static Object getMapObject(String key, Object mapObject) {
		Map<?, ?> map = (Map<?, ?>)mapObject;
		//Map might hold keys that are not of type String.
		//We solve this issue by iterating over the list of map keys and comparing their toString. 
		for (Map.Entry<?, ?> entry : map.entrySet()) {
		    if (entry.getKey().toString().equals(key)){
		        return entry.getValue();
		    }
		}
		logger.severe("Map does not contain a value for the key: " + key);
		throw new RuntimeException("Map object does not contain key: " + key);
	}
	
	public static Object getListClassObject(String index, Object listObject) throws RuntimeException{
		int listIndex = getIndexFromString(index);
		if (listIndex == -1){
		    logger.severe("unable to parse index " + listIndex);
			throw new RuntimeException("unable to parse index " + listIndex);
		}
		List<?> objectList  = (List<?>)listObject;
		if (listIndex >= objectList.size()){
		    logger.severe("Index out of bounds: " + listIndex);
			throw new RuntimeException("Index out of bounds: " + listIndex);
		}
		return objectList.get(listIndex);
	}

	private static int getIndexFromString(String index) {
		int arrayIndex;
		try{
			arrayIndex = Integer.parseInt(index);
		}catch (NumberFormatException e){
			return -1;
		}
		return arrayIndex;
	}

	public static Object getArrayClassObject(String index, Object arrayObject) throws RuntimeException {
		int arrayIndex = getIndexFromString(index);
		if (arrayIndex == -1){
		    logger.severe("unable to parse index " + index);
			throw new RuntimeException("unable to parse index " + index);
		}
		Object[] objectArray = OutputUtils.getArray(arrayObject);
		if (arrayIndex >= objectArray.length){
		    logger.severe("Index out of bounds: " + arrayIndex);
			throw new RuntimeException("Index out of bounds: " + arrayIndex);
		}
		return objectArray[arrayIndex];
	}
	
	private static Method getGetterMethodFromObject(String rawCommand, Class<?> aClass) throws RuntimeException {
		  Method[] methods = aClass.getMethods();
		  String getterCommand = initCommandGetterName(rawCommand, methods);
		  for(Method method : methods){
			  if (getterCommand.equals(method.getName()) && OutputUtils.isValidObjectGetter(method)){
				  return method;
			  }
		  }
		  throw new RuntimeException("No method signature found for method: " + getterCommand);
	}
	
	private static String initCommandGetterName(String rawCommand, Method[] methods) {
		String getterMethodSignature = "get" + Character.toUpperCase(rawCommand.charAt(0)) + rawCommand.substring(1);
		String isMethodSignature = "is" + Character.toUpperCase(rawCommand.charAt(0)) + rawCommand.substring(1);
		for (Method method : methods){
			if (method.getName().equals(getterMethodSignature)){
				return getterMethodSignature;
			}
			if (method.getName().equals(isMethodSignature)){
				return isMethodSignature;
			}
		}
		throw new RuntimeException("No method signature found for command: " + rawCommand);
	}

}
