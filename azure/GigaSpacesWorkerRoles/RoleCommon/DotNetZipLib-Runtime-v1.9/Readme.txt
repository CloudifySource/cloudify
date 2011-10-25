Wed, 01 Apr 2009  19:41

DotNetZip Library and Tools
---------------------------------

DotNetZip is the name of an open-source project that delivers a library
and associated tools for handling ZIP files.  The library allows .NET
applications to read, create and modify ZIP files.  The tools are
example programs that rely on the library, and can be used on any
Windows machine to build or extract ZIP files.


The Microsoft .NET Framework base class library lacks a good set of
classes for creating and reading ZIP files, and Windows itself lacks
full-powered built-in ZIP tools.  DotNetZip fills those needs.


DotNetZip background
---------------------------------

Many people seem to think, incorrectly, that the classes in the
System.IO.Compression namespace, like GZipStream or DeflateStream, can
create or read zip files. Not true.

The System.IO.Compression namespace, starting with v2.0 for the desktop
Framework and v3.5 for the Compact Framework, includes base class
libraries supporting compression within streams - both the Deflate and
Gzip formats are supported. But these classes are not directly useful
for creating compressed ZIP archives.  GZIP is not ZIP. Deflate is not
ZIP.  The GZipStream in System.IO.Compression is able to read and write
GZIP streams, but that is not the same as reading or writing a zip file.
Also, these classes deliver pretty poor compression in practice,
especially with binary data.


Sure, it is possible to read and write zip files, using the classes in
the .NET Framework.  

  - You can do it with the System.IO.Packaging.ZipPackage class, added
    in .NET v3.0. Actually this class lets you create a package file,
    which is a zipfile with a particular internal structure. It includes
    a manifest and some other things.  But the interface is odd and
    confusing if all you want is a regular-old ZIP file.  Also, the
    classes in this namespace do not provide control for things like
    passwords, comments, AES encryption, ZIP64, Unicode, and so on.

  - You can also create and read zip files with the J# runtime, but this
    also has ita drawbacks.  First, J# is going out of support, or may
    be out of support now.  Second, the J# runtime is huge, and you have
    to swallow the whole thing, even if all you want is zip file
    capability.  Also, the J# runtime is based on the java.util.zip
    classes from Java v1.4, dating from 1998.  The runtime hasn't been
    updated in years and still includes bugs in zip file handling. It
    lacks support for AES, for ZIP64, and Unicode.  It is not accessible
    from COM. Finally, the zip classes in the J# runtime are decidedly
    un-dotnet.  There's no support for events, or enumerators to let you
    do things like For Each in VB, or foreach in C#. The interface is
    clunky. It does not feel like a .NET class library, because it isn't
    a .NET class library.  So for all those reasons, J# isn't ideal.

  - You can also rely on P/Invoke to the shell32.dll, and the
    ShellClass. This works but isn't a very intuitive or powerful
    programming interface.  There are no events, so embedding it into a
    Winforms app with a progress bar would be difficult.  Again it lacks
    an easy way to use or access many ZIP features, like encryption or
    ZIP64 or self-extracting archives.  Also, the shell32.dll is
    designed for use within Windows Explorer, and presumes a user
    interface.  In fact in some cases, calling into this DLL to perform
    a ZIP extraction can display a dialog box, so it may not be suitable
    for use within server or "headless" applications.


There are other libraries out there than do zip files for .NET.  But
there are compromises with each one.  Some are commercial and expensive.
Some are slow.  Some are complicated to use.  Some of these options lack
features.  Some of them have more than one of these drawbacks.

DotNetZip provides another option.  It's a very simple-to-use class
library that provides good ZIP file support.  Using this library, you
can write .NET applications that read and write zip-format files,
including files with passwords, Unicode filenames, ZIP64, AES
encryption, and comments.  The library also supports self-extracting
archives.  It is well documented and provides good performance.

Though DotNetZip is implemented in .NET and was originally intended to
provide a managed-code library for ZIP files, you can now use it library
from any COM environment, including Javascript, VBScript, VB6, VBA, PHP,
Perl, and others.  Using DotNetZip, you could generate an AES-encrypted
zip file from within the code of an Excel macro, for example.

DotNetZip works with applications running on PCs with Windows.  There is a
version of this library available for the .NET Compact Framework, too.

DotNetZip is not tested for use with Mono, but many people use the
binary releases with Mono successfully, without change.


License
--------

This software is open source. It is released under the Microsoft Public
License of October 2006.  The use of the "Microsoft Public License" does
not mean it is licensed by Microsoft.  See the License.txt file for
details.



What is DotNetZip?  and How is it packaged?
---------------------------------------------

DotNetZip includes a managed library for dealing with ZIP files, and
a managed library doing ZLIB compression. 

The former depends upon the capabilities included in the latter.

For each DLL, there is a version for the regular .NET Framework and
another for the Compact Framework.




Using the Zip Class Library: The Basics
----------------------------------------

First, there are examples included in the source package, and in the
class reference documentation in the .CHM file, and on the web.  These
examples illustrate how to read and write zip files, with all the
various features.  The examples here are just the basics.

The main type you will use to fiddle with zip files is the ZipFile
class. Full name: Ionic.Zip.ZipFile.  You use this to create, read, or
update zip files.  There is also a ZipOutputStream class, that offers a
different metaphor.

The simplest way to create a ZIP file in C# looks like this:

      using(ZipFile zip= new ZipFile())
      {
        zip.AddFile(filename);
        zip.Save(NameOfZipFileTocreate); 
      }


Or in VB.NET, like this: 

     Using zip As ZipFile = New ZipFile
         zip.AddFile(filename)
         zip.Save("MyZipFile.zip")
     End Using



The simplest way to Extract all the entries from a zipfile looks
like this: 
      using (ZipFile zip = ZipFile.Read(NameOfExistingZipFile))
      {
        zip.ExtractAll(args[1]);
      }

But you could also do something like this: 

      using (ZipFile zip = ZipFile.Read(NameOfExistingZipFile))
      {
        foreach (ZipEntry e in zip)
        {
          e.Extract();
        }
      }
      

Or in VB, extraction would be like this: 
     Using zip As ZipFile = ZipFile.Read(NameOfExistingZipFile)
         zip.ExtractAll
     End Using

Or this: 
     Using zip As ZipFile = ZipFile.Read(NameOfExistingZipFile)
        Dim e As ZipEntry
        For Each e In zip
            e.Extract
        Next
     End Using


That covers the basics.  There are a number of other options for using
the class library.  For example, you can read zip archives from streams,
or you can create (write) zip archives to streams, or you can extract
into streams.  You can apply passwords for weak encryption.  You can
specify a code page for the filenames and metadata of entries in an
archive.  You can rename entries in archives, and you can add or remove
entries from archives.  You can set up save and read progress
events. You can do LINQ queries on the Entries collection. 
Check the doc for complete information, or use Visual Studio's
intellisense to explore some of the properties and methods on the
ZipFile class.

Another type you may use is ZipEntry. This represents a single entry
within a ZipFile.  To add an entry to a zip file, you never have to
directly instantiate a ZipEntry.  But you may wish to modify the
properties of an entry within a zip file, and you do that by twiddling
the properties on the ZipEntry instance.  

The following code adds a file as an entry into a ZipFile, then renames
the entry within the zip file:

      using(ZipFile zip= new ZipFile())
      {
        ZipEntry e = zip.AddFile(filename);
        e.FileName = "RenamedFile.txt";
        zip.Save(NameOfZipFileTocreate); 
      }

Extracting a zip file that was created in this way will produce a file
called "RenamedFile.txt", regardless of the name of the file originally
added to the ZipFile.


The second class that you can use to create zip files is the
ZipOutputStream.  To use it, wrap it around a stream, and write to it. 

      using(var s = new ZipOutputStream(output))
      {
        s.PutNextEntry("entry1.txt");
        byte[] buffer = Encoding.ASCII.GetBytes("This is the content for entry #1.");
        s.Write(buffer, 0, buffer.Length);
      }

Unlike the ZipFile class, the ZipOutputStream class can only create zip
files. It cannot read or update zip files.




Pre-requisites to run Apps that use DotNetZip
----------------------------------------------

to run desktop applications that depend on DotNetZip:
 .NET Framework 2.0 or later


to run smart device applications that depend on DotNetZip:
  .NET Compact Framework 2.0 or later





In more detail: The Zip Class Library
----------------------------------------------

The Zip class library is packaged as Ionic.Zip.DLL for the regular .NET
Framework and Ionic.Zip.CF.dll for the Compact Framework.  The Zip
library allows applications to create, read, and update zip files. 

This library uses the DeflateStream class to compress file data,
and extends it to support reading and writing of the metadata -
the header, CRC, and other optional data - defined or required
by the zip format spec.

The key object in the class library is the ZipFile class.  Some of the
important methods on it:

      - AddItem - adds a file or a directory to a zip archive
      - AddDirectory - adds a directory to a zip archive
      - AddFile - adds a file to a zip archive
      - AddFiles - adds a set of files to a zip archive
      - Extract - extract a single element from a zip file
      - Read - static methods to read in an existing zipfile, for
               later extraction
      - Save - save a zipfile to disk

There is also a supporting class, called ZipEntry.  Applications can
enumerate the entries in a ZipFile, via ZipEntry.  There are other
supporting classes as well.  Typically, 80% of apps will use just the
ZipFile class, and will not need to directly interact with these other
classes. But they are there if you need them. 

If you want to create or read zip files, the Ionic.Zip.DLL assembly is
the one you want.

When building apps that do zip stuff, you need to add a reference to
the Ionic.Zip.dll in Visual Studio, or specify Ionic.Zip.dll with the
/R flag on the CSC.exe or VB.exe compiler line.




In more detail: The Zlib Class Library
-----------------------------------------

The Zlib class library is packaged as Ionic.Zlib.DLL for the regular .NET
Framework and Ionic.Zlib.CF.dll for the Compact Framework.  The ZLIB
library does compression according to IETF RFC's 1950 and 1951.
See http://www.ietf.org/rfc/rfc1950.txt

The key classes are: 

  ZlibCodec - a class for Zlib (RFC1950/1951) encoding and decoding.
        This low-level class does deflation and inflation on buffers.

  DeflateStream - patterned after the DeflateStream in
        System.IO.Compression, this class supports compression
        levels and other options.


If you want to simply compress (deflate) raw block or stream data, this 
library is the thing you want.  

When building apps that do zlib stuff, you need to add a reference to
the Ionic.Zlib.dll in Visual Studio, or specify Ionic.Zlib.dll with the
/R flag on the CSC.exe or VB.exe compiler line.


NB: If your application does both Zlib and Zip stuff, you need only add
a reference to Ionic.Zip.dll.  Ionic.Zip.dll includes all the capability
in Ionic.Zlib.dll.  It's a superset. 





Namespace changes for DotNetZip
-----------------------------------------

The namespace for the DotNetZip classes is Ionic.Zip.
Classes are like:
  Ionic.Zip.ZipFile
  Ionic.Zip.ZipEntry
  Ionic.Zip.ZipException
  etc

(check the .chm file for the full list)

For the versions prior to v1.7, the namespace DotNetZip was Ionic.Utils.Zip.
The classes were like so:
  Ionic.Utils.Zip.ZipFile
  Ionic.Utils.Zip.ZipEntry
  etc

If you have code that depends on an older version of the library, with
classes in the Ionic.Utils.Zip namespace), a simple namespace
replacement will allow your code to compile against the new version of
the library.
  

In addition to the Zip capability, DotNetZip includes capability (new
for v1.7).  For Zlib, the classes are like this:
  Ionic.Zlib.DeflateStream
  Ionic.Zlib.ZlibStream
  Ionic.Zlib.ZlibCodec
  ...

(again, check the .chm file for the full list)




About Directory Paths
---------------------------------

One important note: the ZipFile.AddXxx methods add the file or
directory you specify, including the directory.  In other words,
logic like this:
    ZipFile zip = new ZipFile();
    zip.AddFile("c:\\a\\b\\c\\Hello.doc");
    zip.Save(); 

...will produce a zip archive that contains a single entry, or file, and
that file is stored with the relative directory information.  When you
extract that file from the zip, either using this Zip library or winzip
or the built-in zip support in Windows, or some other package, all those
directories will be created, and the file will be written into that
directory hierarchy.  At extraction time, if you were to extract that
file into a directory like c:\documents, then resulting file would be
named c:\documents\a\b\c\Hello.doc .

This is by design.

If you don't want that directory information in your archive,
then you need to use the overload of the AddFile() method that 
allows you to explicitly specify the directory used for the entry 
within the archive: 

    zip.AddFile("c:\\a\\b\\c\\Hello.doc", "files");
    zip.Save();
    
This will create an archive with an entry called "files\Hello.doc", 
which contains the contents of the on-disk file located at
c:\a\b\c\Hello.doc .  

If you extract that file into a directory e:\documents, then the
resulting file will be called e:\documents\files\Hello.doc . 

If you want no directory at all, specify "" (the empty string).
Specifying null (Nothing in VB) will include all the directory hierarchy
in the filename, as in the orginal case.  



Dependencies
---------------------------------

Originally, this library was designed to depend upon the built-in 
System.IO.Compression.DeflateStream class for the compression.  This
proved to be less than satisfactory because the built-in compression
library did not support compression levels and also was not available on
.NET CF 2.0.

As of v1.7, the library includes a managed code version of zlib, the
library that produces RFC1950 and RFC1951 compressed streams.  Within
that version of zlib, there is also a DeflateStream class which is
similar to the built-in System.IO.Compression.DeflateStream, but more
flexible, and often more effective as well.

As a result, this library depends only on the .NET Framework v2.0, or the
.NET Compact Framework v2.0.




The Documentation
--------------------------------------------

There is a single .chm file for all of the DotNetZip library features,
including Zip and Zlib stuff.  If you only use the Zlib stuff, then you
should focus on the doc in the Ionic.Zlib namespace.  If you are
building apps for mobile devices running the Compact Framework, then
ignore the pieces that deal with SaveSelfExtractor() and AES.

Consult the help file for more specifics here. 

In some cases, upon opening the .chm file for DotNetZip, the help
items tree loads, but the contents are empty. You may see an error:
"This program cannot display the webpage."  or, "Address is invalid."
If this happens, it's likely that you've encountered a problem with Windows
protection of files downloaded from less trusted locations. To work around 
this, within Windows Explorer, right-click on the CHM file, select properties, 
and Unblock it, using the button in lower part of properties window.



The Zip Format
---------------------------------
The zip format is described by PKWare, at
  http://www.pkware.com/business_and_developers/developer/popups/appnote.txt

Every valid zipfile conforms to this specification.  For example, the
spec says that for each compressed file contained in the zip archive,
the zipfile contains a byte array of compressed data.  (The byte array
is something the DeflateStream class can produce directly.)  But the
zipfile also contains header and "directory" information - you might
call this "metadata".  In other words, the zipfile must contain a list
of all the compressed files in the archive. The zipfile also contains
CRC checksums, and can also contain comments, and other optional
attributes for each file.  These are things the DeflateStream class -
either the one included in the .NET Framework Class Library, or the one
embedded in this library - does not read or write.




Which DLL to use?
--------------------------------
The binary releases of DotNetZip include multiple distinct DLLs or
assemblies.  Which one should you use?

The likely answer is:  use Ionic.Zip.dll.

That's the mainstream library, the full library, and it includes all the
capability.  If you have particular requirements, like you want a
smaller library, or you want to exclude the Self-Extracting stuff, or
you only want the ZLIB capability, then you may want to choose a
different assembly.

Here's a summary of the options.


Usage scenario                                 Reference this DLL
------------------------------------------------------------------
reading or writing Zip files                   Ionic.Zip.dll

raw block or stream compression                Ionic.Zlib.dll

both raw compression as well as reading        Ionic.Zip.dll
   or writing Zip files                        

reading or writing Zip files on Compact        Ionic.Zip.CF.dll
     Framework 

raw compression on Compact Framework           Ionic.Zlib.CF.dll

both raw compression as well as reading        Ionic.Zip.CF.dll
   or writing Zip files on CF

reading or writing Zip files, but never        Ionic.Zip.Reduced.dll
  creating a self-extracting archive 



Never reference both Ionic.Zlib.dll and Ionic.Zip.dll in the same
application.  If your application does both Zlib and Zip stuff, you need
only add a reference to Ionic.Zip.dll.  Ionic.Zip.dll includes all the
capability in Ionic.Zlib.dll.  You always need to reference only a
single Ionic DLL, regardless whether you use Zlib or Zip or both.




Self-Extracting Archive support
--------------------------------

The Self-Extracting Archive (SFX) support in the library allows you to 
create a self-extracting zip archive.  An SFX is both a standard EXE
file *and* a ZIP file.  The exe contains boilerplate program logic to
unzip the embedded zip file.  When the user executes the SFX runs, the
boilerplate application logic just reads the zip content and 
then unzips itself. You can open an SFX in WinZip and other zip tools,
as well, if you want to view it.  

Running the SFX (unpacking from the SFX) requires the .NET Framework
installed on the machine, but does not require the DotNetZip library.

There are two versions of the SFX - one that presents a GUI form, and
another that runs as a console (command line) application.

NB: Creation of SFX is not supported in the Compact Framework version of 
the library.

Also, there is no way, currently, to produce an SFX file that can run on
the .NET Compact Framework.




The Reduced ZIP library
--------------------------------

The full DotNetZip library is currently about 400k in size.  The SFX
(Self-Extracting Archive) support is responsible for more than half the
total size of the library.  Some deployments may wish to omit the SFX
support in order to get a smaller DLL. For that you can rely on the
Ionic.Zip.Reduced.dll.  It provides everything the normal library does,
except the SaveSelfExtractor() method on the ZipFile class.

For size comparisons...


assembly              ~size   comment
-------------------------------------------------------
Ionic.Zlib.dll          86k   {Deflate,GZip,Zlib}Stream and ZlibCodec

Ionic.Zip.dll          500k   includes ZLIB and SFX, and selector, 
                              ComHelper class

Ionic.Zip.Partial.dll  278k   includes SFX, depends on a separate Ionic.Zlib.dll
                              You should probably never reference this
                              DLL directly. It is a interim build output.
                              Included here for comparison purposes only.

Ionic.Zip.Reduced.dll  170k   includes ZLIB but not SFX

Ionic.Zlib.CF.dll       74k   {Deflate,GZip,Zlib}Stream and ZlibCodec
                              (Compact Framework)

Ionic.Zip.CF.dll       160k   includes ZLIB but not SFX (Compact Framework)








Support
--------------------------------------------

There is no official support for this library.  I try to make a good
effort to monitor the discussions and work items raised on the project
portal at:
http://DotNetZip.codeplex.com.





About Intellectual Property
---------------------------------

I am no lawyer, but before using this library in your app, it
may be worth contacting PKWare for clarification on rights and
licensing.  The specification for the zip format includes a
paragraph that reads:

  PKWARE is committed to the interoperability and advancement of the
  .ZIP format.  PKWARE offers a free license for certain technological
  aspects described above under certain restrictions and conditions.
  However, the use or implementation in a product of certain technological
  aspects set forth in the current APPNOTE, including those with regard to
  strong encryption or patching, requires a license from PKWARE.  Please 
  contact PKWARE with regard to acquiring a license.

Contact pkware at:  zipformat@pkware.com 

This library does not do strong encryption as described by PKWare, nor
does it do patching.  But again... I am no lawyer. 


This library also uses a CRC utility class, in modified form,
that was published on the internet without an explicit license.
You can find the original CRC class at:
  http://www.vbaccelerator.com/home/net/code/libraries/CRC32/Crc32_zip_CRC32_CRC32_cs.asp


This library uses a ZLIB implementation that is based on a conversion of
the jzlib project http://www.jcraft.com/jzlib/.  The license and
disclaimer required by the jzlib source license is included in the
relevant source files of DotNetZip, specifically in the sources for the
Zlib module.



Limitations
---------------------------------

There are a few limitations to this library:

 it does not support "multi-disk archives." or "disk spanning"

 The GUI tool for creating zips is functional but basic. This isn't a limitation
 of the library per se.  

 and, I'm sure, many others

But it is a good basic library for reading and writing zipfiles
in .NET applications.

And yes, the zipfile that this example is shipped in, was
produced by this example library. 




Building the Library
============================================

This section is mostly interesting to developers who will work on or
view the source code of DotNetZip, to extend or re-purpose it.  If you
only plan to use DotNetZip in applications of your own, you probably
don't need to concern yourself with the information that follows.





Pre-requisites to build DotNetZip
---------------------------------
 
.NET Framework 3.5 SDK or later
  -or-
Visual Studio 2008 or later

  -and-

ILMerge - a tool from Microsoft that combines
multiple managed assemblies into a single DLL or image.  It is in
similar in some respects to the lib tool in C toolkits.

You can get it here:
  http://www.microsoft.com/downloads/details.aspx?familyid=22914587-b4ad-4eae-87cf-b14ae6a939b0&displaylang=en 





Building DotNetZip with the .NET SDK
-------------------------------------

To build the library using the .NET Framework SDK v3.5,

1. extract the contents of the source zip into a new directory. 

2. be sure the .NET 2.0 SDK, .NET 3.5 runtime, and .NET 2.0 runtime 
   directories are on your path.  These are typically

     C:\Program Files\Microsoft.NET\SDK\v2.0\bin
     c:\windows\Microsoft.NET\Framework\v3.5
       and 
     c:\WINDOWS\Microsoft.NET\Framework\v2.0.50727

   The .NET 3.5 runtime is necessary because building DotNetZip requires
   the csc.exe compiler from NET 3.5. (Using DotNetZip from within C#
   requires the v2.0 csc compiler.)


3. Modify the .csproj files in "Zip Partial DLL" and ZLIB to eliminate 
   mention of the Ionic.pfx file.  

   The various DLLs (Zip Partial, ZLIB, etc.) are signed with my private
   key.  You will want to remove the mention of the private key in the
   project files. I cannot distribute my private key, so don't ask me!
   That would be silly.  So you have to modify the project in order to
   build without the key.
   

4. open a CMD prompt and CD to the DotNetZip directory.

  
5. msbuild 
   
   Be sure you are using the .NET 3.5 version of MSBuild.
   This builds the "Debug" version of the library.  To build the
   "Release" version, do this: 

   msbuild /t:Release

   
6. to clean and rebuild, do
   msbuild /t:clean
   msbuild


7. There are two setup directories, which contain the projects
   necessary to build the MSI file.  Unfortunately msbuild does
   not include support for building setup projects (vdproj). 
   You need Visual Studio to build the setup directories.




Building DotNetZip with Visual Studio
-------------------------------------

To build DotNetZip using Visual Studio 2008,

1. Open the DotNetZip.sln file in vS2008.

2. Remove the dependencies on Ionic.pfx and Ionic.snk.  

   The various DLLs (Zip Partial, ZLIB, etc.) are signed with my private
   key.  You will want to remove the mention of the private key in the
   project files. I cannot distribute my private key, so don't ask me!
   That would be silly.

3. Press F6 to build everything.




The Project Structure and Build approach (ILMERGE)
----------------------------------------------------

There are two distinct projects for building libraries: The ZLIB library
and the ZIP library.  The latter depends on the former.

The Ionic.Zip.dll assembly is constructed by combining the
Ionic.Zlib.dll (From the ZLIB project) with the Ionic.Zip.Partial.dll
(from the "Zip Partial DLL" project) using the ILMerge tool. 

In this way  Ionic.Zip.dll becomes  a strict superset of Ionic.Zlib.dll
and Ionic.Zip.Partial.dll. 


It works like this:
  The zlib library is built and signed  (Ionic.Zlib.dll)
  The "partial" zip library is built and signed (Ionic.Zip.Partial.dll)
  ILmerge is used to combine those two into a single assembly
  (Ionic.Zip.dll), which itself is signed. 
 



The missing Ionic.pfx and Ionic.snk files; Signing the assembly yourself.
-------------------------------------------------------------------------

The binary DLL shipped in the codeplex project is signed by me, Ionic
Shade.  This provides a "strong name" for the assembly, which itself
provides some assurance as to the integrity of the library, and also
allows it to be run within restricted sites, like apps running inside
web hosters.

For more on strong names, see this article:
http://msdn.microsoft.com/en-gb/magazine/cc163583.aspx

Signing is done automatically at build time in the VS2008 project or in
the msbuild build. There
is a .pfx file that holds the crypto stuff for signing the assembly, and
that pfx file is itself protected by a password. There is also an
Ionic.snk file which is referenced by the project, but which I do not
distribute.

People opening the project ask me: what's the password to this .pfx
file?  Where's the .snk file?

Here's the problem; if I give everyone the password to the PFX file or
the .snk file, then anyone can go and build a modified DotNetZip.dll,
and sign it with my key, and apply the same version number.  This means
there could be multiple distinct assemblies with the same signature.
This is obviously not good.  

Since I don't release the ability to sign DLLs with my key, 
the DLL signed with my key is guaranteed to be from me only. If
anyone wants to modify the project and party on it, they have a couple
options: 
  - sign the assembly themselves, using their own key.
  - produce a modified, unsigned assembly 

In either case it is not the same as the assembly I am shipping,
therefore it should not be signed with the same key. 

mmkay? 

As for those options above, here is some more detail:

  1. If you want a strong-named assembly, then create your own PFX file
     and .snk file and modify the appropriate projects to use those new
     files. 

  2. If you don't need a strong-named assembly, then remove all the
     signing from the various projects.

In either case, You will need to modify the "Zip Full DLL" and "Zip CF Full
DLL" projects, as well as the "Zlib" and "Zlib CF" projects.




Building the Help File
--------------------------------------------
The .chm file is built using the Sandcastle Helpfile Builder tool, also
available on CodePlex at http://www.codeplex.com/SHFB .  It is built
from in-code xml documentation. 

If you want to build the help file yourself, you will need to have
Sandcastle from May 2008 (or later, I guess), and SHFB, from February
2009.  Both are free tools available from http://codeplex.com .

The helpfile project is DotNetZip.shfbproj



Examples
--------------------------------------------

The source solution also includes a number of example applications
showing how to use the DotNetZip library and all its features - creating
ZIPs, using Unicode, passwords, comments, and so on.   These will all be
built when you run your build.  



Tests
--------------------------------------------

There are two source projects in the VS Solution that contain Unit
Tests: one for the zlib library and another for the Zip library.
If you develop any new tests for DotNetZip, I'd be glad to look at them.







Origins
============================================

This library is mostly original code. 

There is a GPL-licensed library called SharpZipLib that writes zip
files, it can be found at
http://www.sharpdevelop.net/OpenSource/SharpZipLib/Default.aspx

This library is not based on SharpZipLib.  

I think there may be a Zip library shipped as part of the Mono
project.  This library is also not based on that.

Now that the Java class library is open source, there is at least one
open-source Java implementation for zip.  This implementation is not
based on a port of Sun's JDK code.

There is a zlib.net project from ComponentAce.com.  This library is not
based on that code. 

This library is all new code, written by me, with these exceptions:

 -  the CRC32 class - see above for credit.
 -  the zlib library - see above for credit.



You can Donate
--------------------------------

If you think this library is useful, consider donating to my chosen
cause: The Boys and Girls Club of Southwestern Pennsylvania, in the USA.
(In the past I accepted donations for the Boys and Girls Club of
Washington State, also in the USA.  I've moved, and so changed the
charity.)  I am accepting donations on my paypal account.

http://cheeso.members.winisp.net/DotNetZipDonate.aspx

Thanks. 

