package com.gigaspaces.cloudify.mongodb;

import java.util.Map;

import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;

/**
 * @author uri
 */
public class MongoDBDetailsPlugin extends AbstractMongoPlugin implements Details {

    public Map<String, Object> getDetails(UniversalServiceManagerBean universalServiceManagerBean,
                                          UniversalServiceManagerConfiguration universalServiceManagerConfiguration) throws DetailsException {
        Map<String, Object> details = getData();
        details.put("Database name", dbName);
        return details;
    }
}
