import groovy.util.ConfigSlurper

config = new ConfigSlurper().parse(new File("iisproxy-service.properties").toURL())

println("Uninstalling iisproxy")
new AntBuilder().sequential {

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

println("uninstall completed!")