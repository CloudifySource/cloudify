#!/bin/bash

function setJSHome() {
	SCRIPT_PATH=$(dirname $0)
	pushd . > /dev/null
	cd ${SCRIPT_PATH} > /dev/null
	SCRIPT_PATH=`pwd`;
	JSHOMEDIR="$SCRIPT_PATH/../.."
	popd  > /dev/null
}

function setEnv() {
	. $JSHOMEDIR/bin/setenv.sh
}

function setCloudifyJavaOptions() {

	if [ "${GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR}" == "" ]; then
		LRMI_PORT_RANGE=7010-7110
	else
		LRMI_PORT_RANGE=${GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR}
	fi
	
	CLOUDIFY_DEBUG_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9000 -Xnoagent -Djava.compiler=NONE"
	CLOUDIFY_JAVA_OPTIONS="-Xmx500m -Dcom.gigaspaces.logger.RollingFileHandler.debug-level=WARNING -Dcom.gs.transport_protocol.lrmi.bind-port=${LRMI_PORT_RANGE} ${REST_CLIENT_OPTIONS} ${CLOUDIFY_VERSION_OPTIONS} ${EXT_JAVA_OPTIONS}"
}

function setCloudifyClassPath() {
	VERSION_JAR=${JSHOMEDIR}/lib/required/version.jar
	CLI_JARS=${JSHOMEDIR}/tools/cli/cli.jar
	SIGAR_JARS=${JSHOMEDIR}/lib/platform/sigar/sigar.jar
	GROOVY_JARS=${JSHOMEDIR}/tools/groovy/lib/*
	DSL_JARS=${JSHOMEDIR}/lib/platform/cloudify/*
	
	# Test whether this is jdk or jre
	if [ -f "${JAVA_HOME}/jre/lib/deploy.jar" ]; then
		DEPLOY_JARS=${JAVA_HOME}/jre/lib/deploy.jar
	else
		DEPLOY_JARS=${JAVA_HOME}/lib/deploy.jar
	fi
	
	# Add esc dependencies
	ESC_JARS=
	for jar in `find ${JSHOMEDIR}/lib/platform/esm -mindepth 1 -maxdepth 1 -type f -name \*.jar`
	do
		if [ "${ESC_JARS}" == "" ]; then
			ESC_JARS=${jar}
		else
			ESC_JARS=${ESC_JARS}${CPS}${jar}
		fi
	done	
	
	# Add plugins and dependencies
	PLUGIN_JARS=
    
	for jar in `find ${SCRIPT_PATH}/plugins -mindepth 2 -maxdepth 2 -type f -name \*.jar`
	do
		if [ "${PLUGIN_JARS}" == "" ]; then
			PLUGIN_JARS=${jar}
		else
			PLUGIN_JARS=${PLUGIN_JARS}${CPS}${jar}
		fi
	done
	
	CLOUDIFY_CLASSPATH=${VERSION_JAR}${CPS}${CLI_JARS}${CPS}${DSL_JARS}${CPS}${DEPLOY_JARS}${CPS}${GS_JARS}${CPS}${SIGAR_JARS}${CPS}${GROOVY_JARS}${CPS}${ESC_JARS}${CPS}${PLUGIN_JARS}
}

function setCommandLine() {
	CLI_ENTRY_POINT=org.cloudifysource.shell.GigaShellMain
	COMMAND_LINE="${JAVACMD} ${GS_LOGGING_CONFIG_FILE_PROP} ${RMI_OPTIONS} ${LOOKUP_LOCATORS_PROP} ${LOOKUP_GROUPS_PROP} ${CLOUDIFY_JAVA_OPTIONS} -classpath ${PRE_CLASSPATH}${CPS}${CLOUDIFY_CLASSPATH}${CPS}${POST_CLASSPATH} ${CLI_ENTRY_POINT} $*"
}

function init() {
	setJSHome
	setEnv
	setCloudifyJavaOptions
	setCloudifyClassPath
	setCommandLine $*
}

init $*

# add one padding line before the logo
echo 
${COMMAND_LINE}

