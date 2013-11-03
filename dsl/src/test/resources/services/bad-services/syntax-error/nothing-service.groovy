import java.util.concurrent.TimeUnit


service {
	name "groovyError"
	type "UNDEFINED"
	
	elastic true
	numInstances 1
	maxAllowedInstances 1
	
	lifecycle { 
	
		locator {
			NO_PROCESS_LOCATORS
		} 
			
		monitors {
			def time = System.currentTimeMillis()

			return [
				"time" : time]
		}
		
		
	}
