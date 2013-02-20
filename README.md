[![Build Status](https://secure.travis-ci.org/CloudifySource/cloudify.png)](http://travis-ci.org/CloudifySource/cloudify)
Welcome to Cloudify
======================

The purpose of this README file is to explain how one can easily build fully functional Cloudify version taking source code from GitHub.

For more information visit us at http://www.cloudifysource.org/guide.

To follow and open new issues please use https://cloudifysource.atlassian.net/.

Enjoy!


Requirements
-------------
1. Available Internet connection 
2. Installed Java 6 or higher
3. Installed Ant 1.8 or higher
4. Installed maven 3 or higher 
5. System variable  M2_HOME must be defined and point to maven installation folder.


Build Instructions
------------------
1. make sure that you have git, ant and maven installed.
2. In command line, run the following commands:
```
git clone https://github.com/CloudifySource/cloudify.git
cd cloudify
ant cloudify.zip
```
3. The Cloudify distribution will be available under the 'cloudify/tmp' directory

Import Cloudify to Eclipse
-----------------
1. Make sure that the Maven Eclipse Plugin is installed
2. Import the Cloudify maven project. In the file menu click:
   'Import>Maven>Existing Maven Projects projects' then select: cloudify/cloudify/pom.xml 


Debugging the CLI without cloud plugin support
-----------------
1. To debug the CLI without plugin support
```
cd cloudify/CLI/
mvndebug –e compile exec:java
```
2. Connect the eclipse project to port 8000 and start the remote debugging.

Debugging the CLI with cloud plugin support
-----------------
1. Build cloudify as described above
2. Enable debugging option in the cloudify startup script:
   On Linux edit cloudify/tmp/tools/cli/cloudify.sh and append ${CLOUDIFY_DEBUG_OPTIONS} to the commandline adjacent to 
   ```
   ${CLOUDIFY_JAVA_OPTIONS}
   ```
   On Windows edit cloudify/tmptools/cli/cloudify.bat and append %CLOUDIFY_DEBUG_OPTIONS% to the commandline adjacent to 
   ```
   %CLOUDIFY_JAVA_OPTIONS%
   ```
3. Connect the eclipse project to port 9000 and start the remote debugging.




Copyright and license
----------------------
Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");<br/>
you may not use this file except in compliance with the License.<br/>
You may obtain a copy of the License at 

       http://www.apache.org/licenses/LICENSE-2.0
	   
Unless required by applicable law or agreed to in writing, software<br/>
distributed under the License is distributed on an "AS IS" BASIS,<br/>
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br/>
See the License for the specific language governing permissions and<br/>
limitations under the License.
