/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

config = new ConfigSlurper().parse(new File("mongos.properties").toURL())
osConfig = USMUtils.isWindows() ? config.win32 : config.unix

serviceContext = ServiceContextFactory.getServiceContext()
instanceID = serviceContext.getInstanceId()

println "mongos_install.groovy: Writing mongos port to this instance(${instanceID}) attributes..."
serviceContext.attributes.thisInstance["port"] = config.basePort+instanceID
port = serviceContext.attributes.thisInstance["port"] 
println "mongos_install.groovy: mongos(${instanceID}) port ${port}"


builder = new AntBuilder()
builder.sequential {
	mkdir(dir:"${config.installDir}")
	get(src:"${osConfig.downloadPath}", dest:"${config.installDir}/${osConfig.zipName}", skipexisting:true)
}

if(USMUtils.isWindows()) {
	builder.unzip(src:"${config.installDir}/${osConfig.zipName}", dest:"${config.installDir}", overwrite:true)
} else {
	builder.untar(src:"${config.installDir}/${osConfig.zipName}", dest:"${config.installDir}", compression:"gzip", overwrite:true)
	builder.chmod(dir:"${config.installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}
builder.move(file:"${config.installDir}/${osConfig.name}", tofile:"${config.home}")

println "mongos_install.groovy: Installation of mongos(${instanceID}) ended"