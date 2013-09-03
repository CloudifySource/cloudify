import java.util.concurrent.TimeUnit




service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	lifecycle {

		preStart "echo.groovy preStart"
		locator {
			NO_PROCESS_LOCATORS
		}

	}
}