#! /bin/bash

HOST_NAME=`hostname`
echo -e "#! /bin/bash\n echo $PASSWORD" > $WORKING_HOME_DIRECTORY/password.sh
chmod +x $WORKING_HOME_DIRECTORY/password.sh
SUDO_ASKPASS=$WORKING_HOME_DIRECTORY/password.sh
export SUDO_ASKPASS

# add localhost mapping to /etc/hosts
echo "$MACHINE_IP_ADDRESS $HOST_NAME" | sudo -A tee -a /etc/hosts

# enable current user to run sudo without a password
USER=`whoami`
echo "$USER ALL=(ALL)   NOPASSWD :ALL" | sudo -A tee -a /etc/sudoers

# delete the password script
rm $WORKING_HOME_DIRECTORY/password.sh