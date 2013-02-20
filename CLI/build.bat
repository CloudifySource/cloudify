call mvn -DskipTests=true -f pom-all-local.xml install
copy /y target\cli.jar ..\SGTest\tools\gigaspaces\tools\cli\cli.jar