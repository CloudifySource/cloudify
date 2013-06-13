import java.util.concurrent.TimeUnit




service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1	
	lifecycle { 
	
//		install { throw new Exception("This is a failure test") }
		
		preStart {
			new File(context.serviceDirectory + "/marker.txt").delete()
		}
		start ([
			"Win.*" : "run.bat" ,
			"Lin.*" : "run.sh"])
		
		
		startDetection {
			def file = new File(context.serviceDirectory + "/marker.txt")
			println "Checking if file exists: " + file
			new File(context.serviceDirectory + "/marker.txt").exists()
		}
		
		/**
		locator {
			println "Sleeping for 5 secs"
			sleep(5000)
			def query = "Exe.Cwd.eq=" + context.serviceDirectory+",Args.*.eq=org.codehaus.groovy.tools.GroovyStarter"
			println "qeury is: " + query
			def pids = ServiceUtils.ProcessUtils.getPidsWithQuery(query)
			
			println "LOCATORS GOT: " + pids
			return pids;
		}
		**/
	}

	
}