config = new ConfigSlurper().parse(new File("activemq.properties").toURL())

new AntBuilder().sequential {
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:config.installDir, overwrite:true)
	//dos2unix on the linux script files
	fixcrlf(srcDir:"${config.installDir}/${config.name}/bin", eol:"lf", eof:"remove", excludes:"*.bat *.jar")
	chmod(dir:"${config.installDir}/${config.name}/bin", perm:'+x', excludes:"*.bat *.jar")
}
