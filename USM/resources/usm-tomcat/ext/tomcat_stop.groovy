import org.cloudifysource.dsl.context.ServiceContextFactory
println "tomcat_stop.groovy: About to stop tomcat..."

def serviceContext = ServiceContextFactory.getServiceContext()

def instanceID=serviceContext.instanceId
def home= serviceContext.attributes.thisInstance["home"]
println "tomcat_stop.groovy: tomcat(${instanceID}) home ${home}"

def script= serviceContext.attributes.thisInstance["script"]
if (script) {
println "tomcat_stop.groovy: tomcat(${instanceID}) script ${script}"


println "tomcat_stop.groovy: executing command ${script}"
new AntBuilder().sequential {
	exec(executable:"${script}.sh", osfamily:"unix") {
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		arg(value:"stop")
	}
	exec(executable:"${script}.bat", osfamily:"windows"){
        env(key:"CATALINA_HOME", value: "${home}")
    env(key:"CATALINA_BASE", value: "${home}")
    env(key:"CATALINA_TMPDIR", value: "${home}/temp")
		arg(value:"stop")
	}
}

println "tomcat_stop.groovy: tomcat is stopped"
}