Set objShell = CreateObject("Shell.Application")
Set wshShell = CreateObject("WScript.Shell")

If WScript.Arguments.length >= 1 Then appFilePath = WScript.Arguments(0)
If WScript.Arguments.length >= 2 Then appDir = WScript.Arguments(1)
If WScript.Arguments.length >= 3 Then appArgs = WScript.Arguments(2)

Set objSystemEnv = wshShell.Environment("USER")
If objSystemEnv("_ELEVATED")<>"" then
	WScript.Quit 0
End If

objSystemEnv("_ELEVATED") = "1"
objShell.ShellExecute appFilePath, appArgs, appDir, "runas", 1
objSystemEnv("_ELEVATED") = ""
