package org.cloudifysource.dsl;

import java.util.Map;

import org.cloudifysource.dsl.context.ServiceContext;


/***********
 * All USM plugins implementation should implement this interface.
 * @author barakme
 *
 */
public interface Plugin {

	/******************
	 * Setter for the Service Context of the current service.
	 * @param context the service context.
	 */
	public void setServiceContext(ServiceContext context);
	
	/****************
	 * Setter for the plugin parameters, as defined in the Recipe file.
	 * @param config the plugin parameters.
	 */
    public void setConfig(Map<String, Object> config);
    
}
