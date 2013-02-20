service {
    extend "test_parsing_extend-service.groovy"
	name "test parsing extend two level"

	lifecycle{

		install "install"
			
		start "start"
	}
}


