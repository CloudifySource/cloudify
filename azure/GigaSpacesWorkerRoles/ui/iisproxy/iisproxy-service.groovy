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
		"rewrite_add" : { // expected arguments: name, patternSyntax, pattern, rewriteUrl, stopProcessing
			
			commands = [
			
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /+\"[name='${name}',patternSyntax='${patternSyntax}',stopProcessing='${stopProcessing}']\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /[name='${name}',patternSyntax='${patternSyntax}',stopProcessing='${stopProcessing}'].match.url:\"${pattern}\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /[name='${name}',patternSyntax='${patternSyntax}',stopProcessing='${stopProcessing}'].action.type:\"Rewrite\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/globalRules /[name='${name}',patternSyntax='${patternSyntax}',stopProcessing='${stopProcessing}'].action.url:\"${rewriteUrl}\" /commit:apphost",
				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
				"${appCmdPath} list config -section:system.webServer/rewrite/outboundRules"
				
			]
			
			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
			
		},
	
		"rewrite_remove" : { // expected arguments: name, patternSyntax, pattern, rewriteUrl, stopProcessing
		
			commands = [
			
				"${appCmdPath} clear config -section:system.webServer/rewrite/globalRules /[name='${name}',patternSyntax='${patternSyntax}',stopProcessing='${stopProcessing}'] /commit:apphost",
				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
				"${appCmdPath} list config -section:system.webServer/rewrite/outboundRules"
				
			]
			
			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
			
		},
		
		
		"rewrite_outbound_add" : { // expected arguments: name, conditionPattern, rewriteUrl
		
			commands = [
			
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -+[name='${name}'] /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].preCondition:\"IsHTML\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].stopProcessing:\"false\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].match.filterByTags:\"A,Area,Base,Form,Frame,Head,IFrame,Img,Input,Link,Script\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].match.pattern:\"^/(.*)\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -+[name='${name}'].conditions.[input='{URL}'] /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].conditions.[input='{URL}'].pattern:\"${conditionPattern}\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].action.type:\"Rewrite\" /commit:apphost",
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules -[name='${name}'].action.value:\"${rewriteUrl}\" /commit:apphost",
				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
				"${appCmdPath} list config -section:system.webServer/rewrite/outboundRules"
				
			]
			
			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
			
		},
		
		"rewrite_outbound_remove" : { // expected arguments: name
		
			commands = [
			
				"${appCmdPath} set config -section:system.webServer/rewrite/outboundRules --[name='${name}'] /commit:apphost",
				
				"${appCmdPath} list config -section:system.webServer/rewrite/globalRules",
				"${appCmdPath} list config -section:system.webServer/rewrite/outboundRules"
				
			]
		
			commands.each { command ->
				println("executing: ${command}")
				println(command.execute().text)
			}
		
		}
	])
       
}

