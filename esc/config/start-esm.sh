#! /bin/bash

#############################################################################
# This script starts a Gigaspaces agent for use with the Gigaspaces
# Elastic Scaling Module. The node will either run a LUS, ESM and GSM
# if this is the first node, or no services other then the GSA if it is not
#
# Parameters:
# 	$1 - Ip of the head node that runs a LUS and ESM. May be my IP.
#   $2 - 'agent' if this node should join an already running node. Otherwise, 
#			any value.
#   $3 - The IP of this server (Useful if multiple NICs exist)
#############################################################################

export EXT_JAVA_OPTIONS="-Xmx1024m -Xms1024m"

# Some distros do not come with unzip built-in
if [ ! -f "/usr/bin/unzip" ]; then
	chmod +x /opt/gs-files/unzip
	chmod +x /opt/gs-files/unzipsfx
	
	cp /opt/gs-files/unzip /usr/bin
	cp /opt/gs-files/unzipsfx /usr/bin
    
fi

if [ ! -d "/opt/java" -o /opt/gs-files/java.zip -nt /opt/java  ]; then
	rm -rf /opt/java
	mkdir /opt/java
	unzip -q /opt/gs-files/java.zip -d /opt/java
	chmod -R 777 /opt/java	
fi

if [ ! -d "/opt/gigaspaces" -o /opt/gs-files/gigaspaces.zip -nt /opt/gigaspaces ]; then

	rm -rf /opt/gigaspaces
	mkdir /opt/gigaspaces
	unzip -q /opt/gs-files/gigaspaces.zip -d /opt/gigaspaces
	chmod -R 777 /opt/gigaspaces
	mv /opt/gigaspaces/*/* /opt/gigaspaces	

fi

if [ ! -f /opt/gigaspaces/lib/platform/esm/rackspace_esm.jar -o /opt/gs-files/esm.zip -nt /opt/gigaspaces/lib/platform/esm/rackspace_esm.jar ]; then

	rm -f /opt/gigaspaces/lib/platform/esm/*
	unzip -q /opt/gs-files/esm.zip -d /opt/gigaspaces/lib/platform/esm
	chmod -R 777 /opt/gigaspaces/lib/platform/esm

fi

# UPDATE SETENV SCRIPT...
echo Updating environment script
cd /opt/gigaspaces/bin

sed -i '1i export JAVA_HOME=/opt/java' setenv.sh
sed -i "1i export NIC_ADDR=$3" setenv.sh
sed -i "1i export LOOKUPLOCATORS=$1" setenv.sh

# DISABLE LINUX FIREWALL
service iptables save
service iptables stop
chkconfig iptables off

# Restart network cards - This is a recurring problem on Rackspace
# service network restart

# START THE LOOKUP SERVICE AND ESM
if [ "$2" = "agent" ]; then
	nohup ./gs-agent.sh gsa.lus=0 gsa.global.lus=0 gsa.gsm=0 gsa.global.gsm=0 gsa.esm=0 gsa.gsc=0 > /dev/null &
else
	nohup ./gs-agent.sh gsa.lus=1 gsa.global.lus=0 gsa.gsm=1 gsa.global.gsm=0 gsa.esm=1 gsa.gsc=0 > /dev/null &	
fi	

exit 0