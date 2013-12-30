#! /bin/bash

HOST_NAME=`hostname`
echo "$MACHINE_IP_ADDRESS $HOST_NAME" | sudo -A tee -a /etc/hosts

#disable firewall
service iptables stop
