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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudifysource.dsl.Plugin;
import org.cloudifysource.dsl.context.ServiceContext;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
/**
 * @author uri
 * Base class for all mongo plugins
 */
public abstract class AbstractMongoPlugin implements Plugin {

    private final Log log = LogFactory.getLog(getClass());

    private final static String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_DB_NAME = "admin";

    protected ServiceContext serviceContext;

    protected Map<String, Object> config;

    protected String host;
    protected Integer port;
    protected String dbName;
    protected DB db;
    protected boolean initialized = false;
    /**
	* Invoked by USM. Initaizes the <code>ServiceContext</code>
	*/
	@Override
	public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }
     /**
	 * Initializes the plugin config Map as specified in the Recipe
	 */
	@Override
	public void setConfig(Map<String, Object> config) {
        this.config = config;
        
    }

    private <T> T get(final DBObject dbo, final String key) {
        final String[] keys = key.split("\\.");
        DBObject current = dbo;
        Object result = null;
        for (int i = 0; i < keys.length; i++) {
            result = current.get(keys[i]);
            if (i + 1 < keys.length) {
                current = (DBObject) result;
            }
        }
        return (T) result;
    }
    /**
	* Translates Mongo Server Status into <code>Map<String,String></code>
	*/
    protected Map<String, Object> getData() {
        if (!initialized) init();
        Map<String, Object> data = new HashMap<String, Object>();

        CommandResult result = db.command("serverStatus");
        Map<String, String> dataSpec = (Map<String, String>) config.get("dataSpec");
        if (dataSpec != null) {
            for (Map.Entry<String, String> entry : dataSpec.entrySet()) {
            	Object value = get(result, entry.getValue());
            	data.put(entry.getKey(), value);
            }
        }
        return data;
    }
	/**
	* Opens connection to mongoDB
	*/
    public void init() {
        try {
            host = (String) config.get("host");
            if (host == null) host = DEFAULT_HOST;

            log.info("AbstractMongoPlugin.init: using host " + host);
            int instanseID = serviceContext.getInstanceId();
            log.info("AbstractMongoPlugin.init: InstanceId is " + instanseID);
                                               
            port = (Integer)serviceContext.getAttributes().getThisInstance().get("port");
            log.info("AbstractMongoPlugin.init:port is " + port.intValue());
                                   
            dbName = (String) config.get("dbName");            
            if (dbName == null) dbName = DEFAULT_DB_NAME;
            log.info("AbstractMongoPlugin.init:Connecting to mongodb " +dbName+ "("+host+","+port+")...");            
            Mongo mongo = new Mongo(host,port);              
            db = mongo.getDB(dbName);
            log.info("AbstractMongoPlugin.init:Connected to mongodb " +dbName);
            initialized = true;            
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
