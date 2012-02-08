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


try {

serviceContext = ServiceContextFactory.getServiceContext()



instanceID = serviceContext.getInstanceId()
println "mongod_start.groovy: mongod instanceID is ${instanceID}"

home= serviceContext.attributes.thisInstance["home"]
println "mongod_start.groovy: mongod(${instanceID}) home ${home}"

script= serviceContext.attributes.thisInstance["script"]
println "mongod_start.groovy: mongod(${instanceID}) script ${script}"

def port = serviceContext.attributes.thisInstance["port"] 

println "mongod_start.groovy: mongod(${instanceID}) port ${port}"

dataDir = "${home}/data"
println "mongod_start.groovy: mongod(${instanceID}) dataDir ${dataDir}"

println "mongod_start.groovy: Running mongod(${instanceID}) script ${script} ..."

new AntBuilder().sequential {
	//creating the data directory 	
	mkdir(dir:"${dataDir}")

	exec(executable:"${script}") {
		arg line:"--shardsvr"
		arg line:"--dbpath \"${dataDir}\""
        arg line:"--port ${port}"
	}
}

println "mongod_start.groovy: mongod(${instanceID}) script ${script} ended"


} catch (Throwable t) {
org.codehaus.groovy.runtime.StackTraceUtils.sanitize(t);
t.printStackTrace();
throw t;
}

