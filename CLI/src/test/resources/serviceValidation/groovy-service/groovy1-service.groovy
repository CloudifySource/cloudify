import java.util.concurrent.TimeUnit


service {
	name "groovy1"
	icon "icon.png"
	type "WEB_SERVER"
	elastic true
	lifecycle { start "run.groovy" }

	compute {
		template "Cloudify_Server"
	}
	
}