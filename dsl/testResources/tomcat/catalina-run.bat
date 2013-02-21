set CATALINA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dweb.port=80
set CATALINA_HOME=%~dp0install
set CLASSPATH=
install\bin\catalina.bat run