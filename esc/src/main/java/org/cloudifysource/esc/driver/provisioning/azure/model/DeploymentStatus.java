package org.cloudifysource.esc.driver.provisioning.azure.model;

/**
 * 
 * @author elip
 *
 */
public enum DeploymentStatus {
	
	/**
	 * 
	 */
	STARTING {
		
		/**
		 * @return - the string representation of the enum.
		 */
		public String toString() {
			return "Starting";
		}
	} , 
	
	/**
	 * 
	 */
	RUNNING {
		
		/**
		 * @return - the string representation of the enum.
		 */
		public String toString() {
			return "Running";
		}
	}
	
}
