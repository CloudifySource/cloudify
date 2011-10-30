package com.gigaspaces.cloudify.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.monitors.MonitorException;

/**
 * @author uri
 */
public class MongoDBMonitorsPlugin extends AbstractMongoPlugin implements Monitor  {

    public Map<String, Number> getMonitorValues(UniversalServiceManagerBean universalServiceManagerBean,
                                                UniversalServiceManagerConfiguration universalServiceManagerConfiguration) throws MonitorException {
    	Map<String, Object> data = getData();
    	Map<String, Number> monitorMap = new HashMap<String, Number>();
    	for (Map.Entry<String, Object> entry : data.entrySet()) {
    		if(NumberUtils.isNumber((String) entry.getValue())){
    			Number number = NumberUtils.createNumber((String) entry.getValue());
    			monitorMap.put(entry.getKey(), number);
    		}
		}
        return monitorMap;
    }

}
