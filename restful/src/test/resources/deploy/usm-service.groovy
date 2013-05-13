package apps.USM.usm.staticstorage.faulty

import java.util.concurrent.TimeUnit

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;

import com.gigaspaces.internal.sigar.SigarHolder;


service {
	name "groovy"
	type "WEB_SERVER"
	elastic true
	numInstances 1
	maxAllowedInstances 2
	
	isolationSLA {
		global {
			instanceCpuCores 0
			instanceMemoryMB 128
			useManagement false
		}
	}
	
	lifecycle { 

		init { println "This is the init event" }
		preInstall {println "This is the preInstall event" }
		postInstall {println "This is the post install event"}
		preStart {println "This is the preStart event" }
		start "run.groovy" 
		preStop {println "This is the preStop event" }
		postStop {println "This is the postStop event" }
	}
	
	compute {
		
		template "SMALL_LINUX"	
	}

    storage {

        template "SMALL_BLOCK"
    }
}