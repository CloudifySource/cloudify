package com.gigaspaces.cloudify.mongodb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gigaspaces.cloudify.dsl.Plugin;
import com.gigaspaces.cloudify.dsl.context.ServiceContext;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 * @author uri
 */
public abstract class AbstractMongoPlugin implements Plugin {

    private final Log log = LogFactory.getLog(getClass());

    private final static String DEFAULT_HOST = "localhost";
    private final static int DEFAULT_PORT = 27017;
    private static final String DEFAULT_DB_NAME = "admin";

    protected ServiceContext serviceContext;

    protected Map<String, Object> config;

    protected String host;
    protected Integer port;
    protected String dbName;
    protected DB db;
    protected boolean initialized = false;

    public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    protected Integer getPortFromFile(String portFilePath) {
        BufferedReader reader = null;
        try {
            File portFile = new File(serviceContext.getServiceDirectory() + "/" + portFilePath);
            log.info("Port file path: " + portFile.getAbsolutePath());
            if (portFile.exists()) {

                reader = new BufferedReader(new FileReader(portFile));
                String portAsString = reader.readLine();
                return Integer.parseInt(portAsString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
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

    public void init() {
        try {
            host = (String) config.get("host");
            if (host == null) host = DEFAULT_HOST;
            port = (Integer) config.get("port");
            if (port == null) {
                String portFile = (String) config.get("portFile");
                if (portFile != null) {
                    port = getPortFromFile(portFile);
                }
                if (port == null) {
                    port = DEFAULT_PORT;
                }
            }
            dbName = (String) config.get("dbName");
            if (dbName == null) dbName = DEFAULT_DB_NAME;
            Mongo mongo = new Mongo(host, port);
            db = mongo.getDB(dbName);
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
