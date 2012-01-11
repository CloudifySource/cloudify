package com.gigaspaces.cloudify.mongodb;

import java.util.Map;

import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.details.DetailsException;



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
