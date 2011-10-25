config = new ConfigSlurper().parse(new File("tomcat.properties").toURL())

//download apache tomcat
new AntBuilder().sequential {	
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:config.installDir, overwrite:true)
	//get(src:config.applicationWarUrl, dest:config.applicationWar, skipexisting:true)
	//copy(todir: "${config.home}/webapps", file:config.applicationWar, overwrite:true)
	chmod(dir:"${config.home}/bin", perm:'+x', includes:"*.sh")
}

println "Replacing default tomcat port with port ${config.port}"
serverXmlFile = new File("${config.home}/conf/server.xml") 
serverXmlText = serverXmlFile.text	
portStr = "port=\"${config.port}\""
serverXmlFile.text = serverXmlText.replace('port="8080"', portStr) 