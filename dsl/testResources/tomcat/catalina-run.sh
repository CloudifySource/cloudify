echo executing start script
export CATALINA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dweb.port=80"
export CATALINA_HOME="$PWD/install"
echo CATALINA_HOME is $CATALINA_HOME
set CLASSPATH=
chmod +x install/bin/*  
echo calling catalina.sh run
install/bin/catalina.sh run
