service {
	name "simpleStorage"
	type "UNDEFINED"
	
	lifecycle {

		//init { println "This is the init event" }

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		
		// preStart {println "This is the preStart event" }

		start (["Linux":"run.sh"])

		postStart {println "This is the postStart event" }

		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}
	
		compute {
		template "SMALL_LINUX"
	}
	
		storage {
		template "SMALL_BLOCK"
	}
}