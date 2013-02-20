@echo off
set CLASSPATH=

echo creating cassandra schema in 10 seconds
ping 1.2.3.4 -n 1 -w 10000 > nul 2>&1

if not exist createschema.log goto createschema
del /q createschema.log > nul 2>&1

:createschema
call install\bin\cassandra-cli.bat -host localhost -port 9160 -f cassandraSchema.txt >createschema.log 2>&1
if errorlevel 1 goto checkalreadyexists
type createschema.log
echo created cassandra schema
goto end

:checkalreadyexists
findstr /snip /c:"Keyspace already exists" createschema.log
if errorlevel 1 goto err
echo cassandra schema already exists
goto end

:err
echo cannot create cassandra schema
type createschema.log
del /q createschema.log
exit 1

:end
del /q createschema.log
exit 0