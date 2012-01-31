/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.monitors.MonitorException;

/**
 * @author uri
 * Concrete Monitors Plugin - retuerns performance KPIs
 */
public class MongoDBMonitorsPlugin extends AbstractMongoPlugin implements Monitor  {
    /**
	* @return Server Status entries as <code>Map<String,Number></code>
	*/
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
