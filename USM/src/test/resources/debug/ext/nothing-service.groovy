import java.util.concurrent.TimeUnit




service {
	name "nothing"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	lifecycle {

		preInstall "echo.groovy preInstall"
		locator {
			NO_PROCESS_LOCATORS
		}

	}
}