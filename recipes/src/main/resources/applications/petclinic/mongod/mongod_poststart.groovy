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
@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo

config = new ConfigSlurper().parse(new File('mongod.properties').toURL())

println "sleeping for 5 secs"
sleep(5000)

println "Checking connection to mongo on port ${config.port}"
try {
    //check connection 
	mongo = new GMongo("127.0.0.1", config.port)
	db = mongo.getDB("mydb")
	assert db != null 
    println "Connection succeeded"
	mongo.close()
} catch (Exception e) {
    println "Connection Failed!"
	throw e; 
}

