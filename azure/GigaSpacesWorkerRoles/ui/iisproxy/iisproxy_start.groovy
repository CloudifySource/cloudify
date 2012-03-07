import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("iisproxy-service.properties").toURL())

println("Starting iisproxy")
        
new AntBuilder().sequential {
  
    // Restore previously disabled services
    exec(executable:"cmd") {
        arg(value:"/c")
        arg(value:"net")
        arg(value:"start")
        arg(value:"${config.wpActivationService}")
    }

    exec(executable:"cmd") {
        arg(value:"/c")
        arg(value:"net")
        arg(value:"start")
        arg(value:"${config.wwwPublicService}")
    }
}

new File("${config.startedFilename}").write(' ')

println("iisproxy started. Sleeping indefenitley")
while (true) { sleep(60000) }