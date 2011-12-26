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

//Change application pool recycling settings for Application Request Routing.
//http://learn.iis.net/page.aspx/574/install-application-request-routing-version-2/
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"apppool")
        arg(value:"DefaultAppPool")
        arg(value:"-processModel.idleTimeout:\"00:00:00\"")
        arg(value:"/commit:apphost")
    }

//Change application pool process model for Application Request Routing.
//http://learn.iis.net/page.aspx/574/install-application-request-routing-version-2/
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.applicationHost/applicationPools")
        arg(value:"/[name='DefaultAppPool'].recycling.periodicRestart.time:\"00:00:00\"")
        arg(value:"/commit:apphost")
    }

//Increase default timeout to 2 minutes
//http://blogs.iis.net/richma/archive/2010/07/03/502-3-bad-gateway-the-operation-timed-out-with-iis-application-request-routing-arr.aspx
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/proxy")
        arg(value:"/timeout:\"00:02:00\"")
        arg(value:"/commit:apphost")
    }
//disable page caching
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/proxy/cache")
        arg(value:"/enabled:\"false\"")
        arg(value:"/commit:apphost")
    }


if (config.debugMode) {
    // Return detailed error messages to the http client
    exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/httpErrors")
        arg(value:"-errorMode:\"Detailed\"")
        arg(value:"/commit:apphost")
    }
}
else {
    //Disable custom error pages
    exec(executable:"${config.appCmdPath}") {
        arg(value:"clear")
        arg(value:"config")
        arg(value:"-section:system.webServer/httpErrors")
        arg(value:"/commit:apphost")
    }
}
}
println("install completed!")