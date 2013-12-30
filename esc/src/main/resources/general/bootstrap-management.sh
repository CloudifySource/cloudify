#! /bin/bash

#############################################################################
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Cloudify. The agent will function as management depending on the value of $GSA_MODE
#
# Parameters the should be exported beforehand:
# 	$LUS_IP_ADDRESS - Ip of the head node that runs a LUS and ESM. May be my IP. (Required)
#   $GSA_MODE - 'agent' if this node should join an already running node. Otherwise, any value.
#   $NO_WEB_SERVICES - 'true' if web-services (rest, webui) should not be deployed (only if GSA_MODE != 'agent')
#   $NO_MANAGEMENT_SPACE - 'true' if cloudifyManagementSpace should not be deployed (only if GSA_MODE != 'agent')
#   $NO_MANAGEMENT_SPACE_CONTAINER - 'true' if container for cloudifyManagementSpace should not be started(only if GSA_MODE != 'agent')
#   $MACHINE_IP_ADDRESS - The IP of this server (Useful if multiple NICs exist)
# 	$WORKING_HOME_DIRECTORY - This is where the files were copied to (cloudify installation, etc..)
#	$GIGASPACES_LINK - If this url is found, it will be downloaded to $WORKING_HOME_DIRECTORY/gigaspaces.zip
#	$GIGASPACES_OVERRIDES_LINK - If this url is found, it will be downloaded and unzipped into the same location as cloudify
#	$CLOUD_FILE - Location of the cloud configuration file. Only available in bootstrap of management machines.
#	$GIGASPACES_CLOUD_IMAGE_ID - If set, indicates the image ID for this machine.
#	$GIGASPACES_CLOUD_HARDWARE_ID - If set, indicates the hardware ID for this machine.
#	$AUTO_RESTART_AGENT - If set to 'true', will allow to perform reboot of agent machine.
#	$PASSWORD - the machine password.
#############################################################################
# some distro do not have which installed so we're checking if the file exists 
if [ -f /usr/bin/wget ]; then
	DOWNLOADER="wget"
elif [ -f /usr/bin/curl ]; then
	DOWNLOADER="curl"
fi


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

# args:
# $1 download description.
# $2 download link.
# $3 output file.
# $4 the error code.
function download {
	echo Downloading $1 from $2
	if [ "$DOWNLOADER" = "wget" ];then
		Q_FLAG="-q"
		O_FLAG="-O" 
		LINK_FLAG=""
	elif [ "$DOWNLOADER" = "curl" ];then
		Q_FLAG="--silent"
		O_FLAG="-o"
		LINK_FLAG="-O"
	fi
	$DOWNLOADER $Q_FLAG $O_FLAG $3 $LINK_FLAG $2 || error_exit $? $4 "Failed downloading $1"
}

echo Loading Cloudify Environment
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

# Priviliged Script execution
###############################
echo CLOUDIFY_OPEN_FILES_LIMIT is $CLOUDIFY_OPEN_FILES_LIMIT 

if [ -f ${WORKING_HOME_DIRECTORY}/break.bin ] 
then
	echo "Exiting due to break file detected"
	exit 0
fi

function privilegedActions {
	echo Executing priviliged bootstrap actions
	if [ ! -z $CLOUDIFY_OPEN_FILES_LIMIT ] 
	then
		echo setting hard and soft open files ulimit to $CLOUDIFY_OPEN_FILES_LIMIT
		ulimit -HSn $CLOUDIFY_OPEN_FILES_LIMIT
		echo Finished setting open files limit
	
	fi
	if [ -f ${WORKING_HOME_DIRECTORY}/privileged-script.sh ]
	then
		echo executing privileged script
		source ${WORKING_HOME_DIRECTORY}/privileged-script.sh
	fi
	
	echo finished priviliged actions 
}


# first check if we are in an advanced step of priviliged bootstrap
if [ ! -z $PRIVILEGED_BOOTSTRAP_USER ]
then
	echo In second phase of privileged bootstrap
	# phase 2
	privilegedActions
	targetUser="$PRIVILEGED_BOOTSTRAP_USER"
	export PRIVILEGED_BOOTSTRAP_USER=
	echo "export PRIVILEGED_MARKER=on;${WORKING_HOME_DIRECTORY}/bootstrap-management.sh" | sudo -u $targetUser -s
	exit $?
else
	if [ ! -z $PRIVILEGED_MARKER ]
	then
		# finished privileged phase of bootstrap
		export PRIVILEGED_MARKER=
		echo finished privileged phase of bootstrap
	else

		if [ ! -z $CLOUDIFY_OPEN_FILES_LIMIT ] || [ -f "privileged-script.sh" ] 
		then
			# phase 1 - begin priviliged bootstrap process
			echo In first phase of privileged bootstrap
			if [ `whoami` = "root" ] 
			then
				# just run the privileged actions now
				priviligedActions
			else	
				# verify passwordless sudo privileges for current user
				if [ "$GIGASPACES_AGENT_ENV_PRIVILEGED" = "true" ]; then
					sudo -n ls > /dev/null || exit 1
					export PRIVILEGED_BOOTSTRAP_USER=`whoami`
					sudo -E ${WORKING_HOME_DIRECTORY}/bootstrap-management.sh
					exit 0
				else
					# not a password-less sudoer - bootstrap must fail
					exit 115 
				fi
			fi
		else
			echo Standard bootstrap process will be used
		fi
	fi

fi

# Execute pre-bootstrap customization script if exists
run_script "pre-bootstrap"

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
	download "JDK" $GIGASPACES_AGENT_ENV_JAVA_URL $WORKING_HOME_DIRECTORY/java.bin 101
	chmod +x $WORKING_HOME_DIRECTORY/java.bin
	echo -e "\n" > $WORKING_HOME_DIRECTORY/input.txt
	rm -rf ~/java || error_exit $? 102 "Failed removing old java installation directory"
	mkdir ~/java
	cd ~/java
	
	echo Installing JDK
	$WORKING_HOME_DIRECTORY/java.bin < $WORKING_HOME_DIRECTORY/input.txt > /dev/null
	mv ~/java/*/* ~/java || error_exit $? 103 "Failed moving JDK installation"
	rm -f $WORKING_HOME_DIRECTORY/input.txt
    export JAVA_HOME=~/java
    rm -f $WORKING_HOME_DIRECTORY/java.bin || error_exit $? 136 "Failed deleting java.bin from home directory"
fi  

export EXT_JAVA_OPTIONS="-Dcom.gs.multicast.enabled=false"

if [ ! -z "$GIGASPACES_LINK" ]; then
	download "cloudify installation" $GIGASPACES_LINK.tar.gz $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz 104
fi

if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
		download "cloudify overrides" $GIGASPACES_OVERRIDES_LINK.tar.gz $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz 105
fi

# Todo: Check this condition
if [ ! -d "~/gigaspaces" -o $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -nt ~/gigaspaces ]; then
	rm -rf ~/gigaspaces || error_exit $? 106 "Failed removing old gigaspaces directory"
	mkdir ~/gigaspaces || error_exit $? 107 "Failed creating gigaspaces directory"

	# 2 is the error level threshold. 1 means only warnings
	# this is needed for testing purposes on zip files created on the windows platform
	tar xfz $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz -C ~/gigaspaces || error_exit_on_level $? 108 "Failed extracting cloudify installation" 2
	rm -f $WORKING_HOME_DIRECTORY/gigaspaces.tar.gz error_exit $? 134 "Failed deleting gigaspaces.tar.gz from home directory"

	# Todo: consider removing this line
	chmod -R 777 ~/gigaspaces || error_exit $? 109 "Failed changing permissions in cloudify installation"
	mv ~/gigaspaces/*/* ~/gigaspaces || error_exit $? 110 "Failed moving cloudify installation"

	if [ ! -z "$GIGASPACES_OVERRIDES_LINK" ]; then
		echo Copying overrides into cloudify distribution
		tar xfz $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz -C ~/gigaspaces || error_exit_on_level $? 111 "Failed extracting cloudify overrides" 2
		rm -f $WORKING_HOME_DIRECTORY/gigaspaces_overrides.tar.gz error_exit $? 135 "Failed deleting gigaspaces_overrides.tar.gz from home directory"
	fi
fi

# if an overrides directory exists, copy it into the cloudify distribution
if [ -d $WORKING_HOME_DIRECTORY/cloudify-overrides ]; then
	cp -rf $WORKING_HOME_DIRECTORY/cloudify-overrides/* ~/gigaspaces
fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd ~/gigaspaces/bin || error_exit $? 112 "Failed changing directory to bin directory"

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
	eval "$GIGASPACES_AGENT_ENV_INIT_COMMAND"
fi

cd ~/gigaspaces/tools/cli || error_exit $? 118 "Failed changing directory to cli directory"

# Removing old nohup.out
if [ -f nohup.out ]; then
	echo Removing old nohup.out
	rm nohup.out
fi

if [ -f nohup.out ]; then
   error_exit 114 "Failed to remove nohup.out, it might be used by another process"
fi

# START AGENT ALONE OR WITH MANAGEMENT
START_COMMAND_ARGS="-timeout 30 --verbose"
if [ "$GSA_MODE" = "agent" ]; then
	ERRMSG="Failed starting agent"
	START_COMMAND="start-agent"
else
	ERRMSG="Failed starting management services"
	START_COMMAND="start-management"
	START_COMMAND_ARGS="${START_COMMAND_ARGS} -cloud-file ${CLOUD_FILE}"
	if [ "$NO_WEB_SERVICES" = "true" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -no-web-services"
	fi
	if [ "$NO_MANAGEMENT_SPACE" = "true" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -no-management-space"
	fi	
	if [ "$NO_MANAGEMENT_SPACE_CONTAINER" = "true" ]; then
		START_COMMAND_ARGS="${START_COMMAND_ARGS} -no-management-space-container"
	fi	
fi	

# Execute post-bootstrap customization script if exists
run_script "post-bootstrap"

if [ "$AUTO_RESTART_AGENT" = "true" ]; then
	# Add agent restart command to scheduled tasks.
	cat <(crontab -l) <(echo "@reboot nohup ~/gigaspaces/tools/cli/cloudify.sh $START_COMMAND $START_COMMAND_ARGS") | crontab -
fi

./cloudify.sh $START_COMMAND $START_COMMAND_ARGS

RETVAL=$?

if [ $RETVAL -ne 0 ]; then
	echo start command failed, exit code is: $RETVAL
	# exit codes that are larger than 200 are not specified by Cloudify. We use the 255 code to indicate a custom error.
	if [ $RETVAL -gt 200 ]; then
		RETVAL=255
	fi
	error_exit $? $RETVAL "$ERRMSG"
fi
exit 0
