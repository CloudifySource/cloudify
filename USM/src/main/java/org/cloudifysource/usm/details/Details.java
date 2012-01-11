package org.cloudifysource.usm.details;

import java.util.Map;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.UniversalServiceManagerConfiguration;


public interface Details extends USMComponent {
	
	/*************
	 * Returns a map of static details. Details are collected using the GigaSpaces Service Grid and are available via the GigaSpaces Admin API.
	 * 
	 * @param usm the USM bean.
	 * @param config the initial configuration of the USM.
	 * @return The details.
	 * @throws DetailsException in case there was an error while generating the details.
	 */
	Map<String, Object> getDetails(UniversalServiceManagerBean usm, UniversalServiceManagerConfiguration config) throws DetailsException;

}
