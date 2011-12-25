using System;
using System.Net;
using Ionic.Zip;
using System.IO;
using System.Diagnostics;

namespace GigaSpaces
{
    public class FileUtils
    {

        /// <summary>
        /// Downloads the specified url, unzips in the specified direcotry and returns the extracted file path of the specified file
        /// </summary>
        /// <param name="url">Zip download link</param>
        /// <param name="outputDirectory">Output directory to unzip</param>
        /// <param name="filename">A mandatory file inside the zip file</param>
        /// <returns></returns>
        public static FileInfo DownloadUnzipAndFindFile(String url, DirectoryInfo outputDirectory, string filename)
        {
            FileInfo file = null;
            FileInfo temp = new FileInfo(Path.Combine(outputDirectory.FullName,Path.ChangeExtension(Path.GetRandomFileName(),".zip")));
            Download(url, temp);
            
            try
            {
                GSTrace.WriteLine("Unzipping " + temp.FullName + " to " + outputDirectory.FullName);
                using (ZipFile zip = ZipFile.Read(temp.FullName))
                {
                    foreach (ZipEntry entry in zip)
                    {
                        
                        if (!entry.IsDirectory)
                        {
                            entry.Extract(outputDirectory.FullName, ExtractExistingFileAction.OverwriteSilently); 
                            if (!entry.IsDirectory && 
                                Path.GetFileName(entry.FileName).Equals(filename, StringComparison.OrdinalIgnoreCase))
                            {
                                file = new FileInfo(Path.Combine(outputDirectory.FullName,entry.FileName));
                                GSTrace.WriteLine("Found required file: " + file.FullName);
                            }
                        }
                    }
                }

                if (file == null)
                {
                    throw new FileNotFoundException(filename + " was not found in " + url.ToString());
                }
            }
            finally
            {
                File.Delete(temp.FullName);
            }
            return file;
        }

        private static void Download(String input, FileInfo output)
        {
            try
            {
                using (WebClient client = new WebClient())
                {
                    GSTrace.WriteLine("Downloading from URL: " + input );
                    client.DownloadFile(input, output.FullName);
                    GSTrace.WriteLine("Download completed to file: " + output);
                }
            }
            catch (System.Net.WebException e)
            {
                throw new WebException("Failed to download " + input + " to " + output, e);
            }
        }

        /**
         * Subst mounts (or aliases) long directory paths into a short letter drive for the current user
         */
        public static void SetSubstDrive(char driveLetter, String path)
        {
            if (String.IsNullOrEmpty(path)) 
            {
                throw new ArgumentNullException("path cannot be empty");
            }

            // Important to remove last backslash. Otherwise it would cause subst to fail with "path not found"
            path = path.TrimEnd('\\');

            if (!Directory.Exists(path))
            {
                GSTrace.WriteLine("Creating directory " + path);
                Directory.CreateDirectory(path);
                if (!Directory.Exists(path))
                {
                    throw new FileNotFoundException(path);
                }
            }

            String existingPath = FileUtils.GetSubstDrive(driveLetter);
            if (existingPath != null)
            {
                if (path.Equals(existingPath)) {
                    return;
                }
                DeleteSubstDrive(driveLetter);
            }
            
            String output = Subst(driveLetter+": " + path);
            if (output.Replace("\n", "").Trim().Length > 0)
            {
                // probable path not found
                throw new FileNotFoundException(output);
            }
        }

        private static String GetSubstDrive(char driveLetter)
        {
            String[] output = Subst("").Split(new char[]{'\n'});
            foreach (var line in output) {
                String[] substOutput = line.Split(new string[] {"=>"},2,StringSplitOptions.None);
                if (substOutput.Length == 2 && substOutput[0].TrimStart().ToLower()[0] == driveLetter.ToString().ToLower()[0]) {
                    return substOutput[1].Trim();
                }
            }
            return null;
        }

        private static void DeleteSubstDrive(char driveLetter)
        {
            Subst(driveLetter+": /D");
        }

        static String Subst(String args)
        {
            var cmd = new FileInfo(Environment.GetEnvironmentVariable("ComSpec"));
            args = "/c subst " + args;
            var process = new Process()
            {
                StartInfo = new ProcessStartInfo(cmd.FullName, args)
                {
                    Verb = "runas", // elevated Admin priveledges
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    WorkingDirectory = cmd.Directory.FullName,
                    RedirectStandardOutput = true,
                    RedirectStandardError = false
                }
            };
            process.Start();
            try
            {
                var sout = process.StandardOutput.ReadToEnd();
                GSTrace.WriteLine("'cmd " + args + "' returned: '" + sout+"'");
                return sout;
            }
            finally
            {
                process.Close();
            }
        }
    }
}
