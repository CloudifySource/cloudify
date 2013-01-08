import java.util.concurrent.TimeUnit;

// test for grab
@Grab(group='commons-lang', module='commons-lang', version='2.4')
import org.apache.commons.lang.WordUtils

service {
	name  WordUtils.capitalize("tomcat")
	type "WEB_SERVER"


	lifecycle { start "tomcat_start.groovy" }


}