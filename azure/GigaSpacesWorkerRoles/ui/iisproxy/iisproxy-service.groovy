service {

    // This service assumed it is run on a 64bit windows 2008 server machine where IIS7 is installed
    // and the rewrite module is installed on it
    name "iisproxy"
    
    lifecycle {
        init "iisproxy_install.groovy"
		start "run.bat"
		postStop "iisproxy_uninstall.groovy"
    }
	
	network {
        port = 80
        protocolDescription ="HTTP"
    }
	
	customCommands ([
		"rewrite_add_external_lb" : { 
			name,port ->
			// expected arguments: name, port (expects a service property: loadBalancerUrl, injected from the worker role
		
			pattern = "^${name}/(.*)"
			rewriteUrl = "${loadBalancerUrl}:${port}/${name}/{R:1}" 

			pattern2 = "^${name}\$"
			rewriteUrl2 = "${loadBalancerUrl}:${port}/${name}" 
	
			commands = [
				
				//rewrite rule for urls with slash after the name such as http://dns.com/travel/something
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /+\"[name='${name}',patternSyntax='ECMAScript',stopProcessing='true']\" /[name='${name}'].match.url:\"${pattern}\" /[name='${name}'].action.type:\"Rewrite\" /[name='${name}'].action.url:\"${rewriteUrl}\" /commit:apphost",

				//rewrite rule for root url without trailing slash after the name such as http://dns.com/travel
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /+\"[name='${name}_rooturl',patternSyntax='ECMAScript',stopProcessing='true']\" /[name='${name}_rooturl'].match.url:\"${pattern2}\" /[name='${name}_rooturl'].action.type:\"Rewrite\" /[name='${name}_rooturl'].action.url:\"${rewriteUrl2}\" /commit:apphost",

				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
			]

			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
		
		},

		"rewrite_remove_external_lb" : { 
			// expected arguments: expected arguments: name
			name,port ->

			commands = [
			
				"${appCmdPath} clear config -section:system.webServer/rewrite/globalRules /[name='${name}'] /commit:apphost",
				"${appCmdPath} clear config -section:system.webServer/rewrite/globalRules /[name='${name}_rooturl'] /commit:apphost",
				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
				
			]
			
			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
			
		}
	
	])
       
}

