import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("iisproxy-service.properties").toURL())

println("Starting uninstallation")
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
    
	// uninstall ARR
    exec(executable:"msiexec") {
        arg(value:"/x")
        arg(value:"${config.installDir}\\${config.arrMsi}")
        arg(value:"/qn")
        arg(value:"/log")
        arg(value:"${config.installDir}\\${config.arrInstallLog}")
    }


}

// Make sure web farm framework has finished installing before we proceed installing ARR
println("Sleeping for 5 seconds")
Thread.sleep(5000)

new AntBuilder().sequential {
	
    // uninstall web farm framework    
    exec(executable:"msiexec") {
        arg(value:"/x")
        arg(value:"${config.installDir}\\${config.webFarmMsi}")
        arg(value:"/qn")
        arg(value:"/log")
        arg(value:"${config.installDir}\\${config.webFarmInstallLog}")
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
	
	// Define outbound rules precondition
	exec(executable:"${config.appCmdPath}") {
        arg(value:"set")
        arg(value:"config")
        arg(value:"-section:system.webServer/rewrite/outboundRules")
        arg(value:"--preConditions.[name='IsHTML']")
        arg(value:"/commit:apphost")
    }
    
}

println("uninstall completed!")