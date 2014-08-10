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

#disable deprecated cloud-init repo. This patch is temporary as this is an exoscale image issue and should be resolved eventually.
os_dist=`cat /etc/*-release`
if [[ $os_dist =~ .*CentOS.* ]]; then
	echo Renaming deprecated repository 'cloud-init.repo'
	mv /etc/yum.repos.d/cloud-init.repo /etc/yum.repos.d/cloud-init.repo.old
fi
