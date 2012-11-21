#! /bin/bash

#############################################################################
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Cloudify. The agent will function as management depending on the value of $GSA_MODE
#
# The file structure of the installation is as follows:
# $BASE_DIR - determined by the remote directory field of the template for the current machine.
# 	|
#	+ config - Cloud configuration files and bootstrap script will be uploaded here. On a management machine,
#				this will include the cloud file and all template related files. On an agent machine,
#				this will include only the files from the upload folder of the machine's cloud template.
#	+ temp - temporary files are placed here. It is safe to delete this folder after the agent starts.
#	+ java - The JDK that will run cloudify is placed here
#   + cloudify - The cloudify installation is placed here.
#
# Parameters the should be exported beforehand:
# 	$LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   $GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#	$NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   $MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
#	$MACHINE_ZONES - This is required if this is not a management machine
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$GIGASPACES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$GIGASPACES_OVERRIDES_LINK - If this url is found, it will be downloaded and unzipped into the same location as cloudify
#	$CLOUD_FILE - Location of the cloud configuration file. Only available in bootstrap of management machines.
#	$NO_WEB_SERVICES - If set to 'true', indicates that the rest and web-ui services should not be deployed in this machine.
#	$GIGASPACES_CLOUD_IMAGE_ID - If set, indicates the image ID for this machine.
#	$GIGASPACES_CLOUD_HARDWARE_ID - If set, indicates the hardware ID for this machine.
#	$PASSWORD - the machine password
#	$GIGASPACES_AGENT_ENV_JAVA_URL - Optional. URL where the JDK installer can be downloaded. Defaults to the
#										cloudify file repository
#############################################################################

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$2 : error code: $1"
	exit ${1}
}

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error 
# $3 the threshold to exit on
#
# if (last_error_code [$1]) >= (threshold [$3]) the provided message[$2] is printed and the script
# exists with the provided error code ($1)
function error_exit_on_level {
	if [ ${1} -ge ${3} ]; then
		error_exit ${1} ${2}
	fi
}

## Set uup the directories
TEMP_DIR = $BASE_DIR/temp
JAVA_DIR = $BASE_DIR/java
CLOUDIFY_DIR = $BASE_DIR/cloudify
CONFIG_DIR = $BASE_DIR/config

rm -rf $JAVA_DIR/*
rm -rf $CLOUDIFY_DIR/*
rm -rf $TEMP_DIR/*

mkdir TEMP_DIR

JAVA_32_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-i586.bin"
JAVA_64_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-x64.bin"

# If not JDK specified, determine which JDK to install based on hardware architecture
if [ -z "$GIGASPACES_AGENT_ENV_JAVA_URL" ]; then
	ARCH=`uname -m`
	echo Machine Architecture -- $ARCH
	if [ "$ARCH" = "i686" ]; then
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_32_URL
	elif [ "$ARCH" = "x86_64" ]; then
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_64_URL
	else 
		echo Unknown architecture -- $ARCH -- defaulting to 32 bit JDK
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_32_URL
	fi
	
fi  

# in some cases, it is easier to just push the java installer over ssh
# Use in conjunction with the NO_INSTALL value to skip downloading a JDK.
if [ -f "$CONFIG_DIR/java.bin"]; then
	cp $CONFIG_DIR/java.bin $TEMP_DIR/java.bin
fi

if [ "$GIGASPACES_AGENT_ENV_JAVA_URL" = "NO_INSTALL" ]; then
	echo "JDK will not be installed"
else
	echo Previous JAVA_HOME value -- $JAVA_HOME 
	export GIGASPACES_ORIGINAL_JAVA_HOME=$JAVA_HOME

	echo Downloading JDK from $GIGASPACES_AGENT_ENV_JAVA_URL    
	wget -q -O $TEMP_DIR/java.bin $GIGASPACES_AGENT_ENV_JAVA_URL
	chmod +x $TEMP_DIR/java.bin
	echo -e "\n" > $TEMP_DIR/input.txt
	mkdir $JAVA_DIR
	cd $JAVA_DIR
	
	echo Installing JDK
	$TEMP_DIR/java.bin < $TEMP_DIR/input.txt > /dev/null
	mv $JAVA_DIR/*/* $JAVA_DIR || error_exit $? "Failed moving JDK installation"
    export JAVA_HOME=$JAVA_DIR
fi  

export EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

# in some cases, it is easier to just push the cloudify installation over ssh
# Use in conjunction with setting the cloudify url to an enpty string to skip downloading cloudify.
if [ -f "$CONFIG_DIR/cloudify.tar.gz"]; then
	cp $CONFIG_DIR/cloudify.tar.gz $TEMP_DIR/cloudify.tar.gz
fi

if [ ! -z "$GIGASPACES_LINK" ]; then
	echo Downloading cloudify installation from $GIGASPACES_LINK.tar.gz
	wget -q $GIGASPACES_LINK.tar.gz -O $TEMP_DIR/cloudify.tar.gz || error_exit $? "Failed downloading cloudify installation"
fi

if [ -f "$CONFIG_DIR/cloudify_overrides.tar.gz"]; then
	cp $CONFIG_DIR/cloudify_overrides.tar.gz $TEMP_DIR/cloudify_overrides.tar.gz
fi

if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
	echo Downloading cloudify overrides from $GIGASPACES_OVERRIDES_LINK.tar.gz
	wget -q $GIGASPACES_OVERRIDES_LINK.tar.gz -O $TEMP_DIR/cloudify_overrides.tar.gz || error_exit $? "Failed downloading cloudify overrides"
fi

mkdir $CLOUDIFY_DIR || error_exit $? "Failed creating cloudify directory"
	
# 2 is the error level threshold. 1 means only warnings
# this is needed for testing purposes on zip files created on the windows platform 
tar xfz $TEMP_DIR/cloudify.tar.gz -C $CLOUDIFY_DIR || error_exit_on_level $? "Failed extracting cloudify installation" 2 

chmod +x $CLOUDIFY_DIR/bin/* || error_exit $? "Failed changing permissions in cloudify installion"
chmod +x $CLOUDIFY_DIR/tools/cli/* || error_exit $? "Failed changing permissions in cloudify installion"
mv $CLOUDIFY_DIR/*/* $CLOUDIFY_DIR || error_exit $? "Failed moving cloudify installation"
	
if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
	echo Copying overrides into cloudify distribution
	tar xfz $TEMP_DIR/gigaspaces_overrides.tar.gz -C $CLOUDIFY_DIR || error_exit_on_level $? "Failed extracting cloudify overrides" 2
fi

# if an overrides directory exists, copy it into the cloudify distribution
if [ -d $WORKING_HOME_DIRECTORY/cloudify-overrides ]; then
	cp -rf $WORKING_HOME_DIRECTORY/cloudify-overrides/* $CLOUDIFY_DIR
fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd $CLOUDIFY_DIR/bin || error_exit $? "Failed changing directory to bin directory"

sed -i "1i export NIC_ADDR=$MACHINE_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export LOOKUPLOCATORS=$LUS_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export GIGASPACES_CLOUD_IMAGE_ID=$GIGASPACES_CLOUD_IMAGE_ID" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export GIGASPACES_CLOUD_HARDWARE_ID=$GIGASPACES_CLOUD_HARDWARE_ID" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export PATH=$JAVA_HOME/bin:$PATH" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export JAVA_HOME=$JAVA_HOME" setenv.sh || error_exit $? "Failed updating setenv.sh"

# Privileged mode handling
if [ "$GIGASPACES_AGENT_ENV_PRIVILEGED" = "true" ]; then
	# First check if sudo is allowed for current session
	export GIGASPACES_USER=`whoami`
	if [ "$GIGASPACES_USER" = "root" ]; then
		# root is privileged by definition
		echo Running as root
	else
		sudo -n ls > /dev/null || error_exit_on_level $? "Current user is not a sudoer, or requires a password for sudo" 1
	fi
	
	# now modify sudoers configuration to allow execution without tty
	grep -i ubuntu /proc/version > /dev/null
	if [ "$?" -eq "0" ]; then
			# ubuntu
			echo Running on Ubuntu
			if sudo grep -q -E '[^!]requiretty' /etc/sudoers; then
				echo creating sudoers user file
				echo "Defaults:`whoami` !requiretty" | sudo tee /etc/sudoers.d/`whoami` >/dev/null
				sudo chmod 0440 /etc/sudoers.d/`whoami`
			else
				echo No requiretty directive found, nothing to do
			fi
	else
			# other - modify sudoers file
			if [ ! -f "/etc/sudoers" ]; then
					error_exit 101 "Could not find sudoers file at expected location (/etc/sudoers)"
			fi
			echo Setting privileged mode
			sudo sed -i 's/^Defaults.*requiretty/#&/g' /etc/sudoers || error_exit_on_level $? "Failed to edit sudoers file to disable requiretty directive" 1
	fi

fi

# Execute per-template command
if [ ! -z "$GIGASPACES_AGENT_ENV_INIT_COMMAND" ]; then
	echo Executing initialization command
	cd $WORKING_HOME_DIRECTORY
	$SHELL -c $GIGASPACES_AGENT_ENV_INIT_COMMAND
fi

cd $CLOUDIFY_DIR/tools/cli || error_exit $? "Failed changing directory to cli directory"

# START AGENT ALONE OR WITH MANAGEMENT
if [ -f nohup.out ]; then
  rm nohup.out
fi

if [ -f nohup.out ]; then
   error_exit 1 "Failed to remove nohup.out Probably used by another process"
fi

START_COMMAND_ARGS="-timeout 30 --verbose -auto-shutdown"
if [ "$GSA_MODE" = "agent" ]; then
	ERRMSG="Failed starting agent"
	START_COMMAND="start-agent"
	# Check if there any zones to start the agent with
	if [ ! -z "$MACHINE_ZONES" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -zone ${MACHINE_ZONES}"
	fi	
else
	ERRMSG="Failed starting management services"
	START_COMMAND="start-management"
	START_COMMAND_ARGS="${START_COMMAND_ARGS} -cloud-file ${CLOUD_FILE}"
	if [ "$NO_WEB_SERVICES" = "true" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -no-web-services -no-management-space"
	fi
fi	

nohup ./cloudify.sh $START_COMMAND $START_COMMAND_ARGS	

RETVAL=$?
echo cat nohup.out
cat nohup.out
if [ $RETVAL -ne 0 ]; then
  error_exit $RETVAL $ERRMSG
fi
exit 0
