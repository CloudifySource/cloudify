import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("iisproxy-service.properties").toURL())

println("Stopping iisproxy")
new AntBuilder().sequential {
  	
	exec(executable:"cmd") {
        arg(value:"/c")
        arg(value:"net")
        arg(value:"stop")
        arg(value:"${config.wwwPublicService}")
    }

    exec(executable:"cmd") {
        arg(value:"/c")
        arg(value:"net")
        arg(value:"stop")
        arg(value:"${config.wpActivationService}")
    }
}

new File("${config.startedFilename}").deleteOnExit()

