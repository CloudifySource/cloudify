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

portIncrement = instanceID - 1
println "tomcat_install.groovy: Replacing default tomcat port with port ${config.port + portIncrement}"

def serverXmlFile = new File("${home}/conf/server.xml") 
def serverXmlText = serverXmlFile.text	
portReplacementStr = "port=\"${config.port + portIncrement}\""
ajpPortReplacementStr = "port=\"${config.ajpPort + portIncrement}\""
shutdownPortReplacementStr = "port=\"${config.shutdownPort + portIncrement}\""
serverXmlText = serverXmlText.replace("port=\"${config.port}\"", portReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"${config.ajpPort}\"", ajpPortReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"${config.shutdownPort}\"", shutdownPortReplacementStr) 
serverXmlText = serverXmlText.replace('unpackWARs="true"', 'unpackWARs="false"')
serverXmlFile.write(serverXmlText)


println "tomcat_install.groovy: Tomcat installation ended"
