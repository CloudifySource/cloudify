service {
    extend "test_parsing_base-service.groovy"
	name "test parsing extend"

	lifecycle{

		init "test_parsing_extend_install.groovy"
		
		stop {
		  println "stop"
		}
			
	}
}


