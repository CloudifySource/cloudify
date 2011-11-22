service {    
	name "test parsing extend"
	extend "testResources/testparsing/test_parsing_base-service.groovy"

	lifecycle{

		init "test_parsing_extend_install.groovy"
		
		stop {
		  println "stop"
		}
			
	}
}


