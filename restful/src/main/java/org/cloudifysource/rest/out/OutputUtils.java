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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cloudifysource.rest.command.CommandManager;
import org.cloudifysource.rest.util.AdminTypeBlacklist;
import org.cloudifysource.rest.util.PrimitiveWrapper;



/**
 * a util class for inserting various type objects into a predetermined map
 * that is received by reference.
 * @author adaml
 *
 */
public class OutputUtils {

	private static final String URL_ENCODING_FORMAT = "UTF-8";
	private static String hostAddress;
	private static String hostContext;

	private static HashSet<String> getBlackList() {
		HashSet<String> blackList = new HashSet<String>();
		blackList.add("getReplicationStatus com.j_spaces.core.admin.JSpaceAdminProxy");
		blackList.add("getClusterConfigFile com.j_spaces.core.admin.JSpaceAdminProxy");
		blackList.add("getTargetSpaces com.gigaspaces.internal.client.spaceproxy.SpaceProxyImpl");
		blackList.add("getAppDomainId com.gigaspaces.internal.client.spaceproxy.SpaceProxyImpl");
		blackList.add("getDotnetProxyHandleId com.gigaspaces.internal.client.spaceproxy.SpaceProxyImpl");
		blackList.add("getReplicationStatus com.gigaspaces.internal.lrmi.stubs.LRMISpaceImpl");
		blackList.add("getReplicationTarget com.gigaspaces.internal.lrmi.stubs.LRMISpaceImpl");
		blackList.add("getClusterConfigFile com.gigaspaces.internal.lrmi.stubs.LRMISpaceImpl");
		blackList.add("getSpacePump com.gigaspaces.internal.lrmi.stubs.LRMISpaceImpl");
		blackList.add("getLocalConfig com.j_spaces.core.admin.JSpaceAdminProxy");
		blackList.add("getReplicationStatus com.gigaspaces.reflect.$GSProxy10");
		blackList.add("getReplicationStatus com.gigaspaces.reflect.$GSProxy12");
		blackList.add("getClusterConfigFile com.gigaspaces.reflect.$GSProxy12");
		blackList.add("getReplicationTarget com.gigaspaces.reflect.$GSProxy10");
		blackList.add("getSpacePump com.gigaspaces.reflect.$GSProxy10");
		blackList.add("getReplicationStatus com.gigaspaces.reflect.$GSProxy9");
		blackList.add("getClusterConfigFile com.gigaspaces.reflect.$GSProxy10");
		blackList.add("getThreadSecurityContext com.gigaspaces.internal.client.spaceproxy.SpaceProxyImpl");

		return blackList;
	}

	public static String NULL_OBJECT_DENOTER = "<null>";

	private OutputUtils(){ }

	/**
	 * gets an array object and a reference to the output map.
	 * inserts the parameters of the array into the output map.
	 * @param arrayObject
	 * @param outputMap
	 * @param commandURI
	 * @param commandName
	 */
	public static void outputArrayToMap(Object arrayObject, Map<String, Object> outputMap, String completeURL){
		if (isNull(arrayObject)){
			return;
		}
		arrayObject = getArray(arrayObject);
		int arrayLength = ((Object[]) arrayObject).length;
		String[] uriPathArray = new String[arrayLength];
		for (int i = 0; i < arrayLength; i++){
			uriPathArray[i] = completeURL.concat("/" + i);
		}
		String[] commands = completeURL.split("/");
		outputMap.put(commands[commands.length - 1] + "-Elements", uriPathArray);
		outputMap.put(commands[commands.length - 1] + "-Size", arrayLength);
	}

	private static String getRelativePathURLS(String uriPathArray) {

		int contextIndex = uriPathArray.indexOf(getHostContext() + "/admin");
		String relativePath = uriPathArray.substring(contextIndex);
		String newUrlPath = getHostAddress() + relativePath;

		return newUrlPath;
	}

	//Helps in dealing with primitive type arrays. returns an Object Array.
	public static Object[] getArray(Object val){
		int arrlength = Array.getLength(val);
		Object[] outputArray = new Object[arrlength];
		for(int i = 0; i < arrlength; ++i){
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}
	public static void outputListToMap(Object listObject, Map<String, Object> outputMap, String completeURL){
		if (isNull(listObject)) {
			return;
		}
		int listSize = ((List<?>)listObject).size();
		String[] uriPathList = new String[listSize];
		for (int i = 0; i < listSize; i++){
			uriPathList[i] = completeURL.concat("/" + i);
		}
		String[] commands = completeURL.split("/");
		outputMap.put(commands[commands.length - 1].concat("-Size"), uriPathList);

	}

	public static void outputMapToMap(Object mapObject, Map<String, Object> outputMap, String completeURL){
		if (isNull(mapObject)){
			return;
		}
		Map<?, ?> map = (Map<?, ?>)mapObject;
		int mapSize = map.size();
		String[] uriPathArray = new String[mapSize];
		int i = 0;
		for (Object key : map.keySet()){
			uriPathArray[i] = completeURL.concat("/" + key.toString().replace(" ", "%20"));
			i++;
		}
		String[] commands = completeURL.split("/");
		outputMap.put(commands[commands.length - 1].concat("-Elements"), uriPathArray);
	}

	public static void outputObjectToMap(CommandManager manager, Map<String, Object> outputMap){

		Object object = manager.getFinalCommand().getCommandObject();
		String commandURL = getRelativePathURLS(manager.getCommandURL());
		String commandName = manager.getFinalCommandName();

		simpleOutputObjectToMap(object, commandURL, commandName, outputMap);
	}
	
	private static void simpleOutputObjectToMap(Object object,
			String commandURL, String rawCommandName, Map<String, Object> outputMap){
		Class<?> aClass = object.getClass();

		if (PrimitiveWrapper.is(aClass)) {
			outputMap.put(rawCommandName, object.toString());
			return;
		}

		List<Method> validGetterMethods = getValidGetters(aClass);
		Object resultObject = null;
		String commandName;

		for (Method method : validGetterMethods){
			Class<?> methodReturnType = method.getReturnType();
			commandName = getGetterCommandName(method.getName());
			String nextCommandURL = null;

			if (isDetailsGetter(method)){
				resultObject = safeInvoke(method, object);
				if (!isNull(resultObject)){
					HashMap<String, Object> detailsMap = new HashMap<String, Object>();
					//Recurse to get details result in a new map.
					simpleOutputObjectToMap(resultObject, commandURL + "/" + commandName, commandName, detailsMap);
					outputMap.put(commandName, detailsMap);
				}
			}
			else if (methodReturnType.isArray()){
				resultObject = safeInvoke(method, object);
				nextCommandURL = getNextCommandUrl(resultObject, commandURL, commandName);
				OutputUtils.outputArrayToMap(resultObject, outputMap, nextCommandURL);
			}
			else if (Map.class.isAssignableFrom(methodReturnType)){
				resultObject = safeInvoke(method, object);
				nextCommandURL = getNextCommandUrl(resultObject, commandURL, commandName);
				OutputUtils.outputMapToMap(resultObject, outputMap, nextCommandURL);
			}
			else if (List.class.isAssignableFrom(methodReturnType)){
				resultObject = safeInvoke(method, object);
				nextCommandURL = getNextCommandUrl(resultObject, commandURL, commandName);
				OutputUtils.outputListToMap(resultObject, outputMap, nextCommandURL);
			}
			else if (PrimitiveWrapper.is(methodReturnType)) {
				resultObject = safeInvoke(method, object);
				if (!isNull(resultObject)){
					outputMap.put(commandName, resultObject.toString());
				}

			}else{
				outputMap.put(commandName, commandURL.concat("/" + commandName));
				// Special treatment for enum objects.
				resultObject = safeInvoke(method, object);
				if (resultObject.getClass().isEnum()){
					outputMap.put(commandName + "-Enumerator", resultObject.toString());
				}
				if (object.getClass().isEnum()){
					outputMap.put(commandName + "-Enumerator", object.toString());
				}
			}
		}

	}

	/**
	 * The next command url depends on the parent object type.
	 * if the parent object type is Map, and the next command
	 * returns a DS such as Map, Array or List, then the next command
	 * url should NOT include a duplication of the command name.  
	 * @param parentObject
	 * @param commandURI
	 * @param commandName
	 * @return
	 */
	public static String getNextCommandUrl(Object parentObject, String commandURI, String commandName) {
		if (isNull(parentObject)){
			return commandURI;
		}
		boolean isPreLastObjectMap = Map.class.isAssignableFrom(parentObject.getClass());
		String[] allCommands = commandURI.split("/");
		String lastCommandName = allCommands[allCommands.length - 1];
		String outputUrl = null;
		if (isPreLastObjectMap && commandName.equals(lastCommandName)){
			outputUrl = commandURI;
		}
		else{
			outputUrl = commandURI + "/" + commandName;
		}
		outputUrl = getRelativePathURLS(outputUrl);
//		try {
//			outputUrl = URLEncoder.encode(outputUrl, URL_ENCODING_FORMAT);
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		return outputUrl;
	}

	//Trunk is/get
	private static String getGetterCommandName(String getterName) {
		String commandName = null;
		if (getterName.startsWith("is")){
			commandName = getterName.substring(2);
		}else if (getterName.startsWith("get")){
			commandName = getterName.substring(3);
		}
		return commandName;
	}

	//return a list of valid getters.
	private static List<Method> getValidGetters(Class<?> aClass) {
		Method[] allMethods = aClass.getMethods();
		List<Method> validGetterMethods = new ArrayList<Method>();

		for (Method method : allMethods){
			if (isValidObjectGetter(method)){
				validGetterMethods.add(method);
			}
		}
		return validGetterMethods;
	}

	// e.g. getMemcachedDetails()
	private static boolean isDetailsGetter(Method getter) {
		String name = getter.getName();
		return name.startsWith("get") && name.endsWith("Details");		
	}

	public static boolean isValidObjectGetter(Method method) {
		String methodName = method.getName();
		Class<?> retType = method.getReturnType();

		if (methodName.equals("getClass")) { // irrelevant
			return false;
		}

		// special case: avoid event-related getters by name
		if (methodName.endsWith("Changed") || 
				methodName.endsWith("Removed") || 
				methodName.endsWith("Added")) {
			return false;
		}
		// special case: avoid event-related getters by return value
		if (retType.getCanonicalName().contains(".events")) {
			return false;
		}

		if (method.getParameterTypes().length > 0) {
			return false;
		}

		if (AdminTypeBlacklist.in(retType)) {
			return false;
		}

		// special case: boolean getter
		if (methodName.startsWith("is") && (retType.equals(boolean.class) || retType.equals(Boolean.class)) ) {
			return true;
		}		
		else if (!methodName.startsWith("get")) {
			return false;
		}

		return true;
	}

	public static boolean isNull(Object obj) {
		return obj == null || obj.equals(NULL_OBJECT_DENOTER);
	}

	public static Object safeInvoke(Method method, Object obj) {
		Object retval = null;
		String className = obj.getClass().getName();
		String methodName = method.getName();
		try {
			if (getBlackList().contains(methodName + " " + className)){
				return null;
			}
			if (!Map.class.isAssignableFrom(obj.getClass()) && !obj.getClass().isArray() && !List.class.isAssignableFrom(obj.getClass())){
				retval = method.invoke(obj, (Object[])null);	
			}else{
				return "DataSet " + obj.getClass().getTypeParameters()[0];
			}

		}
		//An IllegalAccessException is thrown when an application tries to reflectively invoke a method,
		//but the currently executing method does not have access to the definition of the specified method.
		catch (IllegalAccessException e) {
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot execute getter function " + method.getName() + ". Reason: " + e.getMessage(), e);
		}
		return retval == null ? NULL_OBJECT_DENOTER : retval;
	}

	public static void setHostAddress(String hostAddress) {
		OutputUtils.hostAddress = hostAddress;
	}

	public static String getHostAddress() {
		return hostAddress;
	}

	public static void setHostContext(String hostContext) {
		OutputUtils.hostContext = hostContext;
	}

	public static String getHostContext() {
		return hostContext;
	}
}
