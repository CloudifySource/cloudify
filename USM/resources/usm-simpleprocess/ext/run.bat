@echo off
echo Script parameters are: %*
echo Printing first five parameters:
echo Script parameter no. 1 is: %1
echo Script parameter no. 2 is: %2
echo Script parameter no. 3 is: %3
echo Script parameter no. 4 is: %4
echo Script parameter no. 5 is: %5


java -Dcom.sun.management.jmxremote.port=9988 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar simplejavaprocess.jar -dieOnParentDeath false
rem java -Dcom.sun.management.jmxremote.port=9988 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar simplejavaprocess.jar %*
