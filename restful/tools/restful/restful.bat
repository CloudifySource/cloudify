@echo off
rem 	RESTFUL_JAVA_OPTIONS 	- Extended java options that are proprietary defined  for Restful such as heap size, system properties or other JVM arguments that can be passed to the JVM command line. 
rem								- These settings can be overridden externally to this script.

title GigaSpaces Restful

@set JSHOMEDIR=%~dp0\..\..

@rem set RESTFUL_JAVA_OPTIONS=
set COMPONENT_JAVA_OPTIONS=%RESTFUL_JAVA_OPTIONS%

@rem The call to setenv.bat can be commented out if necessary.
@call "%~dp0\..\..\bin\setenv.bat"

set LOOKUP_GROUPS_PROP=-Dcom.gs.jini_lus.groups=%LOOKUPGROUPS%

if "%LOOKUPLOCATORS%" == ""  (
set LOOKUPLOCATORS=
)
set LOOKUP_LOCATORS_PROP=-Dcom.gs.jini_lus.locators=%LOOKUPLOCATORS%

set LCP=.
for %%i in ("%JSHOMEDIR%\lib\platform\jetty\*.jar") do call "%JSHOMEDIR%\bin\lcp"  "%%i"
set JETTY_JARS=%LCP%


set COMMAND_LINE=%JAVACMD% %JAVA_OPTIONS% %bootclasspath% %LOOKUP_LOCATORS_PROP% %LOOKUP_GROUPS_PROP% %GS_LOGGING_CONFIG_FILE_PROP% %RMI_OPTIONS% "-Dcom.gs.home=%JSHOMEDIR%" -Djava.security.policy=%POLICY% -Dcom.gigaspaces.logger.RollingFileHandler.time-rolling-policy=monthly -classpath %PRE_CLASSPATH%;%GS_JARS%;%EXT_JARS%;%JETTY_JARS%;%POST_CLASSPATH% org.openspaces.launcher.Launcher

%COMMAND_LINE% -name restful -path %~dp0/rest.war -work ./work -port 8100  %*
