service {    
	name "test parsing extend"
	extend "test_parsing_base-service.groovy"

	lifecycle{

		init "test_parsing_extend_install.groovy"
		
		stop {
		  println "stop"
		}
			
	}
}


