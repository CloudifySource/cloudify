service {
	name "smallLinuxService"
	type "UNDEFINED"
	
	lifecycle {

		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the postInstall event"}
		postStart {println "This is the postStart event" }
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
		shutdown {println "This is the shutdown event" }
	}
	
		compute {
		template "SMALL_LINUX"
	}
}