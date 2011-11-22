service {    
	name "test parsing extend"

	lifecycle{
		extend "testResources/testparsing/test_parsing_base-service.groovy"

		init "test_parsing_extend_install.groovy"
		
		stop {
		  println "stop"
		}
			
	}
}


