package org.openspaces.shell.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.gigaspaces.cloudify.dsl.internal.EventLogConstants;

public class EventLoggingTailer {
	
	private Set<String> eventsSet; 
	
	public EventLoggingTailer(){
		this.eventsSet = new HashSet<String>();
	}
	
	public List<String> getLinesToPrint(List<Map<String, String>> allLines){
		if (allLines == null || allLines.isEmpty()){
			return null;
		}
		String outputMessage;
		List<String> outputList = new ArrayList<String>();
		for (Map<String, String> map : allLines) {
			Map<String, Object> sortedMap = new TreeMap<String, Object>(map);
			if (!eventsSet.contains(sortedMap.toString())){
				eventsSet.add(sortedMap.toString());
				outputMessage = getMessageFromMap(sortedMap);
				outputList.add(outputMessage);
			}
			
		}
		return outputList;
	}

	private String getMessageFromMap(Map<String, Object> map) {
		//TODO:Check nulls
		String cleanEventText = (map.get(EventLogConstants.getEventTextKey())).toString().split(" - ")[1];
		String outputMessage = '[' + map.get(EventLogConstants.getMachineHostNameKey()).toString() 
		+ '/' + map.get(EventLogConstants.getMachineHostAddressKey()) + "] " + cleanEventText;
		return outputMessage;
	}
}
