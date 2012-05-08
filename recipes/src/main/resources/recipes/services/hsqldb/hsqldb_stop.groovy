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
// This is a sample groovy script for closing down a running instance of HSQL

@Grab(group='org.hsqldb', module='hsqldb', version='2.2.4')
@GrabConfig(systemClassLoader=true)

sql = groovy.sql.Sql.newInstance("jdbc:hsqldb:hsql://localhost:9001", "SA", "", "org.hsqldb.jdbc.JDBCDriver")
sql.execute "SHUTDOWN"