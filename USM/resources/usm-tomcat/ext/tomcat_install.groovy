import org.cloudifysource.dsl.context.ServiceContextFactory

def config = new ConfigSlurper().parse(new File("tomcat-service.properties").toURL())
def serviceContext = ServiceContextFactory.getServiceContext()
def instanceID = serviceContext.getInstanceId()


println "tomcat_install.groovy: Installing tomcat..."

def home = "${serviceContext.serviceDirectory}/${config.name}"
def script = "${home}/bin/catalina"

serviceContext.attributes.thisInstance["home"] = "${home}"
serviceContext.attributes.thisInstance["script"] = "${script}"
println "tomcat_install.groovy: tomcat(${instanceID}) home is ${home}"


warUrl= serviceContext.attributes.thisService["warUrl"]
if ( warUrl == null ) {  
	warUrl = "${config.applicationWarUrl}"
}

installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID
applicationWar = "${installDir}/${config.warName}"

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:"${installDir}")
	get(src:"${config.downloadPath}", dest:"${installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${installDir}/${config.zipName}", dest:"${installDir}", overwrite:true)
	move(file:"${installDir}/${config.name}", tofile:"${home}")	
	chmod(dir:"${home}/bin", perm:'+x', includes:"*.sh")
}

if ( warUrl != null && "${warUrl}" != "" ) {    
	new AntBuilder().sequential {	
		echo(message:"Getting ${warUrl} to ${applicationWar} ...")
		get(src:"${warUrl}", dest:"${applicationWar}", skipexisting:false)
		echo(message:"Copying ${applicationWar} to ${home}/webapps ...")
		copy(todir: "${home}/webapps", file:"${applicationWar}", overwrite:true)
	}
}

portIncrement = 0
if (serviceContext.isLocalCloud()) {
  portIncrement = instanceID - 1
  println "tomcat_install.groovy: Replacing default tomcat port with port ${config.port + portIncrement}"
}

def serverXmlFile = new File("${home}/conf/server.xml") 
def serverXmlText = serverXmlFile.text	
portReplacementStr = "port=\"${config.port + portIncrement}\""
ajpPortReplacementStr = "port=\"${config.ajpPort + portIncrement}\""
shutdownPortReplacementStr = "port=\"${config.shutdownPort + portIncrement}\""
serverXmlText = serverXmlText.replace("port=\"8080\"", portReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8009\"", ajpPortReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8005\"", shutdownPortReplacementStr) 
serverXmlText = serverXmlText.replace('unpackWARs="true"', 'unpackWARs="false"')
serverXmlFile.write(serverXmlText)


println "tomcat_install.groovy: Tomcat installation ended"
