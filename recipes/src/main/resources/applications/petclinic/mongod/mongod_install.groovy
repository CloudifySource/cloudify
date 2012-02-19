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

serviceContext = ServiceContextFactory.getServiceContext()

config = new ConfigSlurper().parse(new File("mongod.properties").toURL())
osConfig = USMUtils.isWindows() ? config.win32 : config.unix



instanceID = serviceContext.getInstanceId()

installDir = System.properties["user.home"]+ "/.cloudify/${config.service}" + instanceID


home ="${installDir}/mongodb-${config.version}"

serviceContext.attributes.thisInstance["home"] = "${home}"
println "mongod_install.groovy: mongod(${instanceID}) home is ${home}"

serviceContext.attributes.thisInstance["script"] = "${home}/bin/mongod"
println "mongod_install.groovy: mongod(${instanceID}) script is ${home}/bin/mongod"

serviceContext.attributes.thisInstance["port"] = config.basePort+instanceID

def port = serviceContext.attributes.thisInstance["port"] 
println "mongod_install.groovy: mongod(${instanceID}) port ${port}"


builder = new AntBuilder()
builder.sequential {
	mkdir(dir:"${installDir}")
	get(src:"${osConfig.downloadPath}", dest:"${installDir}/${osConfig.zipName}", skipexisting:true)
}

if(USMUtils.isWindows()) {
	builder.unzip(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", overwrite:true)
} else {
	builder.untar(src:"${installDir}/${osConfig.zipName}", dest:"${installDir}", compression:"gzip", overwrite:true)
	builder.chmod(dir:"${installDir}/${osConfig.name}/bin", perm:'+x', includes:"*")
}

println "mongod_install.groovy: mongod(${instanceID}) moving ${installDir}/${osConfig.name} to ${home}..."
builder.move(file:"${installDir}/${osConfig.name}", tofile:"${home}")

println "mongod_install.groovy: mongod(${instanceID}) ended"
