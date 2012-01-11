package com.gigaspaces.cloudify.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;



/**
 * @author uri
 */
public class MongoDBMonitorsPlugin extends AbstractMongoPlugin implements Monitor  {

    public Map<String, Number> getMonitorValues(UniversalServiceManagerBean universalServiceManagerBean,
                                                UniversalServiceManagerConfiguration universalServiceManagerConfiguration) throws MonitorException {
    	Map<String, Object> data = getData();
    	Map<String, Number> monitorMap = new HashMap<String, Number>();
    	for (Map.Entry<String, Object> entry : data.entrySet()) {
    		if(NumberUtils.isNumber(entry.getValue().toString())){
    			Number number = NumberUtils.createNumber(entry.getValue().toString());
    			monitorMap.put(entry.getKey(), number);
    		}
		}
        return monitorMap;
    }

}
