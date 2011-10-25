#!/bin/bash
# 		RESTFUL_JAVA_OPTIONS 	- Extended java options that are proprietary defined  for Restful such as heap size, system properties or other JVM arguments that can be passed to the JVM command line. 
#								- These settings can be overridden externally to this script.

if [ "${JSHOMEDIR}" = "" ] ; then
  JSHOMEDIR=`dirname $0`/../../
fi
export JSHOMEDIR

# RESTFUL_JAVA_OPTIONS=; export RESTFUL_JAVA_OPTIONS
COMPONENT_JAVA_OPTIONS="${RESTFUL_JAVA_OPTIONS}"
export COMPONENT_JAVA_OPTIONS

# The call to setenv.sh can be commented out if necessary.
. $JSHOMEDIR/bin/setenv.sh

LOOKUP_GROUPS_PROP=-Dcom.gs.jini_lus.groups=${LOOKUPGROUPS}; export LOOKUP_GROUPS_PROP

if [ "${LOOKUPLOCATORS}" = "" ] ; then
LOOKUPLOCATORS=; export LOOKUPLOCATORS
fi
LOOKUP_LOCATORS_PROP="-Dcom.gs.jini_lus.locators=${LOOKUPLOCATORS}"; export LOOKUP_LOCATORS_PROP

for i in "${JSHOMEDIR}"/lib/platform/jetty/*.jar
do
    JETTY_JARS=${JETTY_JARS}$CPS$i
done
export JETTY_JARS

COMMAND_LINE="${JAVACMD} ${JAVA_OPTIONS} $bootclasspath ${RMI_OPTIONS} ${LOOKUP_LOCATORS_PROP} ${LOOKUP_GROUPS_PROP} -Djava.security.policy=${POLICY} -Dcom.gigaspaces.logger.RollingFileHandler.time-rolling-policy=monthly -Dcom.gs.home=${JSHOMEDIR} -classpath "${PRE_CLASSPATH}${CPS}${GS_JARS}${CPS}${EXT_JARS}${CPS}${JETTY_JARS}${CPS}${POST_CLASSPATH}" org.openspaces.launcher.Launcher"

${COMMAND_LINE} -name restful -path ./rest.war -work ./work -port 8080 $*

