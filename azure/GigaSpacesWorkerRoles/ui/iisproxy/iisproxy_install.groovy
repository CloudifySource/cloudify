import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("iisproxy-service.properties").toURL())

println("Starting installation")
new AntBuilder().sequential {
    
    mkdir(dir:config.installDir)

    // download IIS components    
    get(src:config.webFarmMsiDownloadLink, dest:"${config.installDir}/${config.webFarmMsi}", skipexisting:true)
    get(src:config.arrMsiDownloadLink, dest:"${config.installDir}/${config.arrMsi}", skipexisting:true)

    // Stop web services before installation
	
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
    
	
    // Install web farm framework    
    exec(executable:"msiexec") {
        arg(value:"/i")
        arg(value:"${config.installDir}\\${config.webFarmMsi}")
        arg(value:"/qn")
        arg(value:"/log")
        arg(value:"${config.installDir}\\${config.webFarmInstallLog}")
    }

}

// Make sure web farm framework has finished installing before we proceed installing ARR
println("Sleeping for 5 seconds")
Thread.sleep(5000)

new AntBuilder().sequential {
	
    // Install ARR
    exec(executable:"msiexec") {
        arg(value:"/i")
        arg(value:"${config.installDir}\\${config.arrMsi}")
        arg(value:"/qn")
        arg(value:"/log")
        arg(value:"${config.installDir}\\${config.arrInstallLog}")
    }

}
    
// Make sure ARR has finished installing before we proceed
println("Sleeping for 5 seconds")
Thread.sleep(5000)
  
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
    
    // Enable proxy in IIS
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/proxy")
        arg(value:"/enabled:\"True\"")
        arg(value:"/commit:apphost")
    }

    //preserve original host header
    //see: http://stackoverflow.com/questions/1842885/modifying-headers-with-iis7-application-request-routing
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/proxy")
        arg(value:"/preserveHostHeader:\"True\"")
        arg(value:"/commit:apphost")
    }
 
}

println("install completed!")