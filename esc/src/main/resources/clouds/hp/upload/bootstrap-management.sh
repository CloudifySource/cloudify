#! /bin/bash

#############################################################################
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Cloudify. The agent will function as management depending on the value of $GSA_MODE
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
#	$STORAGE_VOLUME_ATTACHED - if set to 'true', storage volume will be mouted. else all storage params will be null.
#	$STORAGE_FORMAT_TYPE - if set, indicates the file system type for formatting the volume before mount.
#	$STORAGE_MOUNT_PATH - if set, points to the path where the storage driver will be mounted.
#	$STORAGE_DEVICE_NAME - if set, indicated the storage device name.
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

echo Checking script path
SCRIPT=`readlink -f $0`
SCRIPTPATH=`dirname $SCRIPT`
echo script path is $SCRIPTPATH


if [ -f ${SCRIPTPATH}/cloudify_env.sh ]; then
	ENV_FILE_PATH=${SCRIPTPATH}/cloudify_env.sh
else
	if [ -f ${SCRIPTPATH}/../cloudify_env.sh ]; then
		ENV_FILE_PATH=${SCRIPTPATH}/../cloudify_env.sh
	else
		echo Cloudify environment file not found! Bootstrapping cannot proceed!
		exit 105
	fi

fi

source ${ENV_FILE_PATH}

if [ "$STORAGE_VOLUME_ATTACHED" = "true" ]; then
	echo Formatting storage volume with fs type ${STORAGE_FORMAT_TYPE} and device name ${STORAGE_DEVICE_NAME} 
	sudo mkfs -t $STORAGE_FORMAT_TYPE $STORAGE_DEVICE_NAME || error_exit $? "Failed formatting storage volume"
	echo Mounting storage volume on path ${STORAGE_MOUNT_PATH}
	mkdir -p ~/$STORAGE_MOUNT_PATH
	sudo mount $STORAGE_DEVICE_NAME ~/$STORAGE_MOUNT_PATH || error_exit $? "Failed mounting storage volume"
	USERNAME=`whoami`
	sudo chown $USERNAME storage/ 
fi

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

if [ "$GIGASPACES_AGENT_ENV_JAVA_URL" = "NO_INSTALL" ]; then
	echo "JDK will not be installed"
else
	echo Previous JAVA_HOME value -- $JAVA_HOME 
	export GIGASPACES_ORIGINAL_JAVA_HOME=$JAVA_HOME

	echo Downloading JDK from $GIGASPACES_AGENT_ENV_JAVA_URL    
	wget -q -O $WORKING_HOME_DIRECTORY/java.bin $GIGASPACES_AGENT_ENV_JAVA_URL || error_exit $? "Failed downloading Java installation from $GIGASPACES_AGENT_ENV_JAVA_URL"
	chmod +x $WORKING_HOME_DIRECTORY/java.bin
	echo -e "\n" > $WORKING_HOME_DIRECTORY/input.txt
	rm -rf ~/java || error_exit $? "Failed removing old java installation directory"
	mkdir ~/java
	cd ~/java
	
	echo Installing JDK
	$WORKING_HOME_DIRECTORY/java.bin < $WORKING_HOME_DIRECTORY/input.txt > /dev/null
	mv ~/java/*/* ~/java || error_exit $? "Failed moving JDK installation"
	rm -f $WORKING_HOME_DIRECTORY/input.txt
    export JAVA_HOME=~/java
fi  

export EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

if [ ! -z "$GIGASPACES_LINK" ]; then
	echo Downloading cloudify installation from $GIGASPACES_LINK.tar.gz
	wget -q $GIGASPACES_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz || error_exit $? "Failed downloading cloudify installation"
fi

if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
	echo Downloading cloudify overrides from $GIGASPACES_OVERRIDES_LINK.tar.gz
	wget -q $GIGASPACES_OVERRIDES_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz || error_exit $? "Failed downloading cloudify overrides"
fi

# Todo: Check this condition
if [ ! -d "~/gigaspaces" -o $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -nt ~/gigaspaces ]; then
	rm -rf ~/gigaspaces || error_exit $? "Failed removing old gigaspaces directory"
	mkdir ~/gigaspaces || error_exit $? "Failed creating gigaspaces directory"
	
	# 2 is the error level threshold. 1 means only warnings
	# this is needed for testing purposes on zip files created on the windows platform 
	tar xfz $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -C ~/gigaspaces || error_exit_on_level $? "Failed extracting cloudify installation" 2 

	# Todo: consider removing this line
	chmod -R 777 ~/gigaspaces || error_exit $? "Failed changing permissions in cloudify installion"
	mv ~/gigaspaces/*/* ~/gigaspaces || error_exit $? "Failed moving cloudify installation"
	
	if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
		echo Copying overrides into cloudify distribution
		tar xfz $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz -C ~/gigaspaces || error_exit_on_level $? "Failed extracting cloudify overrides" 2 		
	fi
fi

# if an overrides directory exists, copy it into the cloudify distribution
if [ -d $WORKING_HOME_DIRECTORY/cloudify-overrides ]; then
	cp -rf $WORKING_HOME_DIRECTORY/cloudify-overrides/* ~/gigaspaces
fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd ~/gigaspaces/bin || error_exit $? "Failed changing directory to bin directory"

sed -i "1i source  ${ENV_FILE_PATH}" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export NIC_ADDR=$MACHINE_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export LOOKUPLOCATORS=$LUS_IP_ADDRESS" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export PATH=$JAVA_HOME/bin:$PATH" setenv.sh || error_exit $? "Failed updating setenv.sh"
sed -i "1i export JAVA_HOME=$JAVA_HOME" setenv.sh || error_exit $? "Failed updating setenv.sh"


# START AGENT ALONE OR WITH MANAGEMENT
if [ -f nohup.out ]; then
  rm nohup.out
fi

if [ -f nohup.out ]; then
   error_exit 1 "Failed to remove nohup.out Probably used by another process"
fi

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
	$GIGASPACES_AGENT_ENV_INIT_COMMAND
fi

cd ~/gigaspaces/tools/cli || error_exit $? "Failed changing directory to cli directory"

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
