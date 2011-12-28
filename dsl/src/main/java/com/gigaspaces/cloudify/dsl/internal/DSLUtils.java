package com.gigaspaces.cloudify.dsl.internal;

import org.openspaces.admin.internal.pu.InternalProcessingUnit;

public class DSLUtils {

 	// The context property set in application DSL files to indicate the directory where the application file itself can be found
	public static final String APPLICATION_DIR = "workDirectory";


    private DSLUtils() {
    	// private constructor to prevent initialization
    }


    public static String getDependencies(InternalProcessingUnit processingUnit) {
    	String dependencies = getContextPropertyValue(processingUnit, CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
    	if (dependencies == null) {
    		return "";
    	}
    	return dependencies;
    }

    public static ServiceTierType getTierType(InternalProcessingUnit processingUnit) {
    	String tierTypeStr = getContextPropertyValue(processingUnit, CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE);
    	if (tierTypeStr == null) {
    		return ServiceTierType.UNDEFINED;
    	}
    	return ServiceTierType.valueOf(tierTypeStr);
    }
    
	public static String getIconUrl(InternalProcessingUnit processingUnit) {
		String iconUrlStr = getContextPropertyValue(processingUnit, CloudifyConstants.SERVICE_EXTERNAL_FOLDER 
															+ CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON);
		if (iconUrlStr == null) {
			return "";
		}
		return iconUrlStr;
	}

	private static String getContextPropertyValue(InternalProcessingUnit processingUnit,
			String contextPropertyKey) {
		String value = processingUnit.getBeanLevelProperties().getContextProperties()
				.getProperty(contextPropertyKey);
		return value;
	}

}
