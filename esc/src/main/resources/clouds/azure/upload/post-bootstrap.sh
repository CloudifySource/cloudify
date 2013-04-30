#! /bin/bash

# add localhost mapping to /etc/hosts

HOST_NAME=`hostname`
echo -e "#! /bin/bash\n echo $PASSWORD" > $WORKING_HOME_DIRECTORY/password.sh
chmod +x $WORKING_HOME_DIRECTORY/password.sh
SUDO_ASKPASS=$WORKING_HOME_DIRECTORY/password.sh
export SUDO_ASKPASS
echo "$MACHINE_IP_ADDRESS $HOST_NAME" | sudo -A tee -a /etc/hosts
rm $WORKING_HOME_DIRECTORY/password.sh