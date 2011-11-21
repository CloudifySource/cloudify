import com.j_spaces.kernel.Environment

config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:config.downloadDir)
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.downloadDir}/${config.zipName}", skipexisting:true)	
	unzip(src:"${config.downloadDir}/${config.zipName}", dest:config.installDir, overwrite:true)	
	mkdir(dir:config.websiteInstallLocation)	
	copy(todir: "${config.home}/lib", file:config.tlcJar, overwrite:true) 
	copy(todir: "${config.home}/conf", file:config.catalinaPropertiesFile, overwrite:true)
	copy(todir: "${config.home}/conf", file:config.contextFile, overwrite:true)
	chmod(dir:"${config.home}/bin", perm:'+x', includes:"*.sh")	
}

new AntBuilder().copy(todir:config.websiteInstallLocation) {
    fileset(dir:config.websiteLocation)
}

println "Replacing default tomcat port with port ${config.port}"
serverXmlFile = new File("${config.home}/conf/server.xml") 
serverXmlText = serverXmlFile.text	
portStr = "port=\"${config.port}\""
serverXmlFile.text = serverXmlText.replace('port="8080"', portStr)

def env = System.getenv()

lookupLocator = env['LOOKUPLOCATORS']
println "Replacing lookup locators with " + lookupLocator
contextXmlFile = new File("${config.home}/conf/context.xml")
contextXmlText = contextXmlFile.text
contextXmlFile.text = contextXmlText.replace('<LOOKUP_LOCATOR>', lookupLocator)


gsHome = Environment.getHomeDirectory()
gsHome = gsHome.replace('\\','/') 
println "Replacing gs home dir with " + gsHome
catalinaPropertiesFile = new File("${config.home}/conf/catalina.properties")
catalinaPropertiesText = catalinaPropertiesFile.text
catalinaPropertiesFile.text = catalinaPropertiesText.replace('<GS_HOME>', gsHome)