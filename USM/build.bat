rmdir /s /q target
call mvn package
if not exist target\usm.jar goto err
copy target\usm.jar ..\SGTest\lib\usm\usm.jar
goto exit
:err
echo BUILD ERROR
:exit