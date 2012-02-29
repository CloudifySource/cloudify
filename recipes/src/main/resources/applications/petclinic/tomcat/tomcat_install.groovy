import org.cloudifysource.dsl.context.ServiceContextFactory

def config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())
def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()


println "tomcat_install.groovy: Installing tomcat..."

def home = "${serviceContext.serviceDirectory}/${config.name}"
def script = "${home}/bin/catalina"

serviceContext.attributes.thisInstance["home"] = "${home}"
serviceContext.attributes.thisInstance["script"] = "${script}"
println "tomcat_install.groovy: tomcat(${instanceID}) home is ${home}"



//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:"${config.installDir}")
	get(src:"${config.downloadPath}", dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:"${config.installDir}", overwrite:true)
	move(file:"${config.installDir}/${config.name}", tofile:"${home}")
	get(src:"${config.applicationWarUrl}", dest:"${config.applicationWar}", skipexisting:true)
	copy(todir: "${home}/webapps", file:"${config.applicationWar}", overwrite:true)
	chmod(dir:"${home}/bin", perm:'+x', includes:"*.sh")
}

println "tomcat_install.groovy: Replacing default tomcat port with port ${config.port}"
def serverXmlFile = new File("${home}/conf/server.xml") 
def serverXmlText = serverXmlFile.text	
def portStr = "port=\"${config.port}\""
serverXmlFile.text = serverXmlText.replace('port="8080"', portStr) 

println "tomcat_install.groovy: Tomcat installation ended"