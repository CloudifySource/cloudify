#! /bin/bash

HOST_NAME=`hostname`
echo "$MACHINE_IP_ADDRESS $HOST_NAME" | sudo -A tee -a /etc/hosts

#disable firewall
os=`uname -a`
if [[ $os =~ .*Ubuntu.* ]]; then
	service ufw stop
else
	service iptables stop
fi