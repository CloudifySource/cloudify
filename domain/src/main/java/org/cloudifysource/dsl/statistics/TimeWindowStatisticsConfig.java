package org.cloudifysource.dsl.statistics;

import java.util.HashMap;
import java.util.Map;

public class TimeWindowStatisticsConfig {

	private Map<String, String> properties = new HashMap<String, String>();

	private Long timeWindowSeconds = 60l;

	private Long minimumTimeWindowSeconds;

	private Long maximumTimeWindowSeconds;

	public Map<String,String> getProperties() {
		return this.properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	   
    /**
     * @return the timeWindowSeconds
     */
    public Long getTimeWindowSeconds() {
        return this.timeWindowSeconds;
    }
    /**
     * @param timeWindowSeconds the timeWindowSeconds to set
     */
    public void setTimeWindowSeconds(long timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }
    /**
     * @return the minimumTimeWindowSeconds
     */
    public Long getMinimumTimeWindowSeconds() {
    	return this.minimumTimeWindowSeconds;
    }
    
    /**
     * @param minimumTimeWindowSeconds the minimumTimeWindowSeconds to set
     */
    public void setMinimumTimeWindowSeconds(long minimumTimeWindowSeconds) {
        this.minimumTimeWindowSeconds = minimumTimeWindowSeconds;
    }
    /**
     * @return the maximumTimeWindowSeconds
     */
    public Long getMaximumTimeWindowSeconds() {
        return this.maximumTimeWindowSeconds;
    }

    /**
     * @param maximumTimeWindowSeconds the maximumTimeWindowSeconds to set
     */
    public void setMaximumTimeWindowSeconds(long maximumTimeWindowSeconds) {
    	this.maximumTimeWindowSeconds = maximumTimeWindowSeconds;
    }
	
}
