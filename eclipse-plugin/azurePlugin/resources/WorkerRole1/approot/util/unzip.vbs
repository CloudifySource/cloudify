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

' To unzip a file using shell32.dll APIs

Dim args
set args = WScript.Arguments 

Dim zipFileName, folderName, pathFilter
If (args.Length = 2) Then
    zipFileName = args(0)
    folderName = args(1)
    pathFilter = ""
ELSE 
    If (args.Length = 3) Then
        zipFileName = args(0)
        folderName = args(1)
        pathFilter = args(2)
    Else
        WScript.Echo "Invalid arguments. Usage: UnzipUtility.vbs <zipFileName> <folderName> [pathFilter]"
        WScript.Quit(1)
    End If
End If

' Create FileSystemObject for file related operations
Dim fso
Set fso= CreateObject("Scripting.FileSystemObject")

' Create output folder if does not exist
If Not fso.FolderExists(folderName) Then 
    fso.CreateFolder(folderName)
End If

' Create shell32 application instance
Dim oShell
set oShell = CreateObject("Shell.Application")

' Get absolute path names for files and folders
Dim zipFileAbsolutePathName, folderAbsolutePathName
zipFileAbsolutePathName = fso.GetAbsolutePathName(zipFileName)
folderAbsolutePathName = fso.GetAbsolutePathName(folderName)

' Get zip file handle
Dim zip
Set zip = oShell.NameSpace(zipFileAbsolutePathName)

' Get output folder handle
Dim ex
Set ex = oShell.NameSpace(folderAbsolutePathName)

If Len(pathFilter) > 0 Then
    ' Only specified folder has to be extracted
    Dim pathParts, pathPart, folderToExtract

    ' Search for specified folder in the zip file
    Set folderToExtract = zip
    pathParts = Split(pathFilter, "\")
    For Each pathPart in pathParts
        Dim itemFound
        itemFound = False

	Dim fileItem
        For Each fileItem in folderToExtract.items
            If fileItem.Name = pathPart Then
                itemFound = True
                Set folderToExtract = fileItem.GetFolder

		' Item found, break the for loop
                Exit For
            End If
        Next
        If (itemFound = False) Then
           WScript.Echo "UnzipUtility.vbs: Path not found in zip"
           WScript.Quit(1)
        End If
    Next

    ' Unzip only specified folder, without showing any UI/popup
    ex.CopyHere folderToExtract.items, 20
Else
    ' Unzip all files, without showing any UI/popup
    ex.CopyHere zip.items, 20
End If

WScript.Echo "Successfully extracted the zip file."