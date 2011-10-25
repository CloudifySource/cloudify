package org.openspaces.rest.out;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openspaces.rest.command.CommandManager;


public class OutputDispatcher {

	
	public static Map<String, Object> outputResultObjectToMap(CommandManager manager, String contextPath, String hostContext){
		OutputUtils.setHostAddress(contextPath);
		OutputUtils.setHostContext(hostContext);
		Map<String, Object> outputMap = new HashMap<String, Object>();
		Map<String, Object> sortedMap;
		Object object = manager.getFinalCommand().getCommandObject();
		if (OutputUtils.isNull(object)){
			outputMap.put(manager.getFinalCommandName(), OutputUtils.NULL_OBJECT_DENOTER);
			return outputMap;
		}
		String nextCommandURL = OutputUtils.getNextCommandUrl(manager.getFinalCommand().getPreviousCommandObject(), manager.getCommandURL(), manager.getFinalCommandName());
		Class<?> aClass = object.getClass();
		if (aClass.isArray()){
			OutputUtils.outputArrayToMap(object, outputMap, nextCommandURL);
		}else if (Map.class.isAssignableFrom(aClass)) {
			OutputUtils.outputMapToMap(object, outputMap, nextCommandURL);
		}else if (List.class.isAssignableFrom(aClass)) {
			OutputUtils.outputListToMap(object, outputMap, nextCommandURL);
		}else{
			OutputUtils.outputObjectToMap(manager, outputMap);
		}
		sortedMap = new TreeMap<String, Object>(outputMap);
		
		return sortedMap;
	}


	

	
}
