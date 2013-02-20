service {    
	name "test parsing extend"

	lifecycle{
		extend "test_parsing_base-service.groovy"

		init "test_parsing_extend_install.groovy"
		
		stop {
		  println "stop"
		}
			
	}
}


