/*******************************************************************************
* Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.usm.USMUtils

println("Checking operating system's compatibility with mongod")
if (!USMUtils.isWindows()){
	String command = "uname -a"
	Process pr = command.execute()
	pr.waitFor();
	if (pr.in.text.contains("2007")){
		throw new Exception("The Linux legacy-static builds are needed for older systems." 
			+ "Try and run mongod from the commandline and if you get a floating point exception, use a legacy-static build.");  
	}
}
println("op system is compatible with mongod")

serviceContext = ServiceContextFactory.getServiceContext()
instanceIdFile = new File("./instanceId.txt")
instanceIdFile.text = serviceContext.instanceId

config = new ConfigSlurper().parse(new File("mongod.properties").toURL())
osConfig = USMUtils.isWindows() ? config.win32 : config.linux

portFile = new File("./port.txt")
portFile.text = config.port

builder = new AntBuilder()
builder.sequential {
	mkdir(dir:config.installDir)
	get(src:osConfig.downloadPath, dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(USMUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:config.installDir, compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:config.home)
