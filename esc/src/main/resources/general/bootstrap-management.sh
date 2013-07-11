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
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$GIGASPACES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$GIGASPACES_OVERRIDES_LINK - If this url is found, it will be downloaded and unzipped into the same location as cloudify
#	$CLOUD_FILE - Location of the cloud configuration file. Only available in bootstrap of management machines.
#	$NO_WEB_SERVICES - If set to 'true', indicates that the rest and web-ui services should not be deployed in this machine.
#	$GIGASPACES_CLOUD_IMAGE_ID - If set, indicates the image ID for this machine.
#	$GIGASPACES_CLOUD_HARDWARE_ID - If set, indicates the hardware ID for this machine.
#	$PASSWORD - the machine password
#############################################################################

# args:
# $2 the error code of the last command (should be explicitly passed)
# $3 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$3 : error code: $2"
	exit ${2}
}

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error 
# $3 the threshold to exit on
#
# if (last_error_code [$1]) >= (threshold [$4]) (defaults to 0), the script
# exits with the provided error code [$2] and the provided message [$3] is printed
function error_exit_on_level {
	if [ ${1} -ge ${4} ]; then
		error_exit ${2} ${3}
	fi
}

# args:
# $1 the name of the script. must be located in the upload folder.
function run_script {
    FULL_PATH_TO_SCRIPT="$WORKING_HOME_DIRECTORY/$1.sh"
    if [ -f $FULL_PATH_TO_SCRIPT ]; then
        chmod +x $FULL_PATH_TO_SCRIPT
        echo Running script $FULL_PATH_TO_SCRIPT
        $FULL_PATH_TO_SCRIPT
        RETVAL=$?
        if [ $RETVAL -ne 0 ]; then
          error_exit $RETVAL "Failed running $1 script"
        fi
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
		error_exit 100 "Cloudify environment file not found! Bootstrapping cannot proceed!"
	fi

fi

source ${ENV_FILE_PATH}
HOME_DIR="/tmp/noak/byon"

# Execute pre-bootstrap customization script if exists
run_script "pre-bootstrap"

#JAVA_32_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-i586.bin"
#JAVA_64_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-x64.bin"
JAVA_32_URL="http://tarzan/builds/GigaSpacesBuilds/tools/quality/java/1.6.0_32/jdk-6u32-linux-i586.bin"
JAVA_64_URL="http://tarzan/builds/GigaSpacesBuilds/tools/quality/java/1.6.0_32/jdk-6u32-linux-x64.bin"

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
	wget -q -O $WORKING_HOME_DIRECTORY/java.bin $GIGASPACES_AGENT_ENV_JAVA_URL || error_exit $? 101 "Failed downloading Java installation from $GIGASPACES_AGENT_ENV_JAVA_URL"
	chmod +x $WORKING_HOME_DIRECTORY/java.bin
	echo -e "\n" > $WORKING_HOME_DIRECTORY/input.txt
	rm -rf $HOME_DIR/java || error_exit $? 102 "Failed removing old java installation directory"
	mkdir $HOME_DIR/java
	cd $HOME_DIR/java
	
	echo Installing JDK
	$WORKING_HOME_DIRECTORY/java.bin < $WORKING_HOME_DIRECTORY/input.txt > /dev/null
	mv $HOME_DIR/java/*/* $HOME_DIR/java || error_exit $? 103 "Failed moving JDK installation"
	rm -f $WORKING_HOME_DIRECTORY/input.txt
    export JAVA_HOME=$HOME_DIR/java
fi  

export EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

if [ ! -z "$GIGASPACES_LINK" ]; then
	echo Downloading cloudify installation from $GIGASPACES_LINK.tar.gz
	wget -q $GIGASPACES_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz || error_exit $? 104 "Failed downloading cloudify installation"
fi

if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
	echo Downloading cloudify overrides from $GIGASPACES_OVERRIDES_LINK.tar.gz
	wget -q $GIGASPACES_OVERRIDES_LINK.tar.gz -O $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz || error_exit $? 105 "Failed downloading cloudify overrides"
fi

# Todo: Check this condition
if [ ! -d "$HOME_DIR/gigaspaces" -o $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -nt $HOME_DIR/gigaspaces ]; then
	rm -rf $HOME_DIR/gigaspaces || error_exit $? 106 "Failed removing old gigaspaces directory"
	mkdir $HOME_DIR/gigaspaces || error_exit $? 107 "Failed creating gigaspaces directory"

	# 2 is the error level threshold. 1 means only warnings
	# this is needed for testing purposes on zip files created on the windows platform
	tar xfz $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -C $HOME_DIR/gigaspaces || error_exit_on_level $? 108 "Failed extracting cloudify installation" 2

	# Todo: consider removing this line
	chmod -R 777 $HOME_DIR/gigaspaces || error_exit $? 109 "Failed changing permissions in cloudify installation"
	mv $HOME_DIR/gigaspaces/*/* $HOME_DIR/gigaspaces || error_exit $? 110 "Failed moving cloudify installation"

	if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
		echo Copying overrides into cloudify distribution
		tar xfz $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz -C $HOME_DIR/gigaspaces || error_exit_on_level $? 111 "Failed extracting cloudify overrides" 2
	fi
fi

# if an overrides directory exists, copy it into the cloudify distribution
if [ -d $WORKING_HOME_DIRECTORY/cloudify-overrides ]; then
	cp -rf $WORKING_HOME_DIRECTORY/cloudify-overrides/* $HOME_DIR/gigaspaces
fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd $HOME_DIR/gigaspaces/bin || error_exit $? 112 "Failed changing directory to bin directory"

sed -i "2i . ${ENV_FILE_PATH}" setenv.sh || error_exit $? 113 "Failed updating setenv.sh"
sed -i "2i export NIC_ADDR=$MACHINE_IP_ADDRESS" setenv.sh || error_exit $? 113 "Failed updating setenv.sh"
sed -i "2i export LOOKUPLOCATORS=$LUS_IP_ADDRESS" setenv.sh || error_exit $? 113 "Failed updating setenv.sh"
sed -i "2i export PATH=$JAVA_HOME/bin:$PATH" setenv.sh || error_exit $? 113 "Failed updating setenv.sh"
sed -i "2i export JAVA_HOME=$JAVA_HOME" setenv.sh || error_exit $? 113 "Failed updating setenv.sh"


# Privileged mode handling
if [ "$GIGASPACES_AGENT_ENV_PRIVILEGED" = "true" ]; then
	# First check if sudo is allowed for current session
	export GIGASPACES_USER=`whoami`
	if [ "$GIGASPACES_USER" = "root" ]; then
		# root is privileged by definition
		echo Running as root
	else
		sudo -n ls > /dev/null || error_exit_on_level $? 115 "Current user is not a sudoer, or requires a password for sudo" 1
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
					error_exit 116 "Could not find sudoers file at expected location (/etc/sudoers)"
			fi
			echo Setting privileged mode
			sudo sed -i 's/^Defaults.*requiretty/#&/g' /etc/sudoers || error_exit_on_level $? 117 "Failed to edit sudoers file to disable requiretty directive" 1
	fi

fi

# Execute per-template command
if [ ! -z "$GIGASPACES_AGENT_ENV_INIT_COMMAND" ]; then
	echo Executing initialization command
	cd $WORKING_HOME_DIRECTORY
	$GIGASPACES_AGENT_ENV_INIT_COMMAND
fi

cd $HOME_DIR/gigaspaces/tools/cli || error_exit $? 118 "Failed changing directory to cli directory"

# Removing old nohup.out
if [ -f nohup.out ]; then
	echo Removing old nohup.out
	rm nohup.out
fi

if [ -f nohup.out ]; then
   error_exit 114 "Failed to remove nohup.out, it might be used by another process"
fi

# START AGENT ALONE OR WITH MANAGEMENT
START_COMMAND_ARGS="-timeout 30 --verbose -auto-shutdown"
if [ "$GSA_MODE" = "agent" ]; then
	ERRMSG="Failed starting agent"
	START_COMMAND="start-agent"
else
	ERRMSG="Failed starting management services"
	START_COMMAND="start-management"
	START_COMMAND_ARGS="${START_COMMAND_ARGS} -cloud-file ${CLOUD_FILE}"
	if [ "$NO_WEB_SERVICES" = "true" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -no-web-services -no-management-space"
	fi
fi	

# Execute post-bootstrap customization script if exists
run_script "post-bootstrap"


nohup ./cloudify.sh $START_COMMAND $START_COMMAND_ARGS

RETVAL=$?
echo cat nohup.out
cat nohup.out

if [ $RETVAL -ne 0 ]; then
	echo in bootstrap-management script, exit code is: $RETVAL
	# exit codes that are larger than 200 are not specified by Cloudify. We use the 255 code to indicate a custom error.
	if [ $RETVAL -gt 200 ]; then
		RETVAL=255
	fi
	error_exit $? $RETVAL "$ERRMSG"
fi
exit 0