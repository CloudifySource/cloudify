@REM Sample run script that exits to trigger a role recycle when java.exe stops running.
@REM If java.exe is not running to begin with, it will assume this is not a Java app deployment and keep it permanently alive.
@REM Customize this script to define your own monitoring and role recycle logic.

@ECHO OFF
util\whileproc.cmd java.exe