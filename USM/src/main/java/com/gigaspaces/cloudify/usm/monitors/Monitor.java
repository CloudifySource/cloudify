package com.gigaspaces.cloudify.usm.monitors;

import java.util.Map;

import com.gigaspaces.cloudify.usm.USMComponent;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerBean;
import com.gigaspaces.cloudify.usm.UniversalServiceManagerConfiguration;

public interface Monitor extends USMComponent {


	/**************
	 * Returns a map of statistics generated for this service. Statistics are
	 * collected using the GigaSpaces Service Grid and are available via the
	 * GigaSpaces Admin API.
	 * 
	 * @param usm
	 *            The USM Bean.
	 * @param config
	 *            The initial USM Configuration.
	 * @return the statistics.
	 * @throws MonitorException
	 *             in case an error was encountered while generating the
	 *             statistics.
	 */
	Map<String, Number> getMonitorValues(UniversalServiceManagerBean usm, UniversalServiceManagerConfiguration config) throws MonitorException;


}
