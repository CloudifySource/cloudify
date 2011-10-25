import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("elasticsearch.properties").toURL())

new AntBuilder().sequential {
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	unzip(src:"${config.installDir}/${config.zipName}", dest:config.installDir, overwrite:true)
	chmod(file:"${config.script}", perm:'+x')
}
