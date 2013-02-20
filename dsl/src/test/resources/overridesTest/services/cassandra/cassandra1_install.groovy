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
import java.util.concurrent.TimeUnit
import org.cloudifysource.dsl.context.ServiceContextFactory

config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())

serviceContext = ServiceContextFactory.getServiceContext()
home = "${serviceContext.serviceDirectory}/${config.unzipFolder}"
instanceID = serviceContext.getInstanceId()
installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceID

new AntBuilder().sequential {
	mkdir(dir:installDir)
	get(src:config.downloadPath, dest:"${installDir}/${config.zipName}", skipexisting:true)
	untar(src:"${installDir}/${config.zipName}", dest:installDir, compression:"gzip")
	move(file:"${installDir}/${config.unzipFolder}", tofile:"${home}")
	chmod(dir:"${home}/bin", perm:'+x', excludes:"*.bat")
}	

