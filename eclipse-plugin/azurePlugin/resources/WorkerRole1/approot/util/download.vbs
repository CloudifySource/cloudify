'*************************************************************
' Copyright 2011 Microsoft Corp
'
' Licensed under the Apache License, Version 2.0 (the "License");
' you may not use this file except in compliance with the License.
' You may obtain a copy of the License at
' 
' http://www.apache.org/licenses/LICENSE-2.0
' 
' Unless required by applicable law or agreed to in writing, software
' distributed under the License is distributed on an "AS IS" BASIS,
' WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
' See the License for the specific language governing permissions and
' limitations under the License.
'*************************************************************

' To download a file using WinHTTP.WinHttpRequest and save using ADODB.Stream

set args = WScript.Arguments 

'Read download URL from arguments
If args.Length >= 1 Then
    downloadURL = args(0)
Else
    WScript.Echo "Invalid arguments. Usage: download.vbs <downloadURL> [<destinationPath>]"
    WScript.Quit(1)
End If

'Read destination path
If args.Length >= 2 Then
    'If provided as argument then read it
    destPath = args(1)
Else 
    'Otherwise, determine file name from URL
    destPath = StrReverse(Split(StrReverse(downloadURL), "/")(0))
End If

'Create WinHttpRequest to download file
Set httpReq = CreateObject("WinHttp.WinHttpRequest.5.1")
httpReq.Open "GET", downloadURL, False
httpReq.Send

'Create FileSystemObject for deleting the file
Set fso= CreateObject("Scripting.FileSystemObject")
If fso.FileExists(destPath) Then
   fso.DeleteFile destPath
End If
Set fso = Nothing

' Save the file using ADODB stream
Set stream = CreateObject("ADODB.Stream")  
stream.Open  
stream.Type = 1  
stream.Write httpReq.ResponseBody
stream.Position  = 0
stream.SaveToFile destPath
stream.Close

set stream=Nothing
set httpReq=Nothing