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
config = new ConfigSlurper().parse(new File('mongoConfig.properties').toURL())

//start mongod
dataDir = "${config.home}/data/cfg"

new AntBuilder().sequential {
	//creating the data directory 	
    delete(file:"${dataDir}/mongod.lock")
	mkdir(dir:dataDir)
	exec(executable:config.script) {
		arg value:"--configsvr"
		arg line:"--dbpath \"${dataDir}\""
		arg line:"--port ${config.port}"
    }
}

