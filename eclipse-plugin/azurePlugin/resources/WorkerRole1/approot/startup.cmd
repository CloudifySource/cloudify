:: *** Sample startup script containing the steps for starting Apache Tomcat and deploying a WAR file. 
:: *** (Last tested with Apache Tomcat 7.0.22)

:: To use the sample, follow these steps:
:: 1) Copy all this content into approot/startup.cmd in the role folder, close this file, and edit the copy
:: 2) Place a JDK distribution as jdk.zip under approot
:: 3) Place an Apache Tomcat 7.x distribution as tomcat7.zip under approot in your project
::    3.1) If you want to download the server into Azure directly from a URL instead, then
::         uncomment the next line and modify the URL as appropriate:
:: cscript /NoLogo "util\download.vbs" "http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.22/bin/apache-tomcat-7.0.22.zip" "tomcat7.zip"

:: 4) Update SERVER_DIR_NAME below as appropriate:
::    (IMPORTANT: There must be no trailing nor leading whitespace around the setting)

SET SERVER_DIR_NAME=apache-tomcat-7.0.23

			
:: *****************************************************************			
:: *** Deployment and startup logic
:: *** (Do not make changes below unless you know what you're doing.

rd "\%ROLENAME%"
mklink /D "\%ROLENAME%" "%ROLEROOT%\approot"
cd /d "\%ROLENAME%"
cscript /NoLogo util\unzip.vbs jdk.zip "%CD%"
cscript /NoLogo util\unzip.vbs webserver.zip "%CD%"
copy %WAR_NAME% "%SERVER_DIR_NAME%\webapps\%WAR_NAME%"
cd "%SERVER_DIR_NAME%\bin"
set JAVA_HOME=\%ROLENAME%\jdk
set JRE_HOME=C:\Program Files\Java\jdk1.6.0_25
set PATH=%PATH%;%JAVA_HOME%\bin
cmd /c startup.bat

@ECHO OFF
if %ERRORLEVEL%==0 exit %ERRORLEVEL%
choice /d y /t 5 /c Y /N /M "*** Windows Azure startup failed - exiting..."
