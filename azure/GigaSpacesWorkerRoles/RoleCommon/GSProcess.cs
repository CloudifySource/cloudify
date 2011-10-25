using System;
using System.Diagnostics;
using System.IO;
using System.Collections.Generic;

namespace GigaSpaces
{
    public class GSProcess : IDisposable
    {
        private readonly Process process = new Process();

        public DirectoryInfo WorkingDirectory { private get; set; }
        public string Command { private get; set; }
        public string Arguments { private get; set; }
        public bool RedirectStandardOutput { private get; set; }
        public bool RedirectStandardError { private get; set; }
        public IDictionary<String,String> EnvironmentVariables { private get; set; }
        
        public GSProcess ()
	    {
            EnvironmentVariables = new Dictionary<String, String>();
	    }
        
        public void Run()
        {
            ProcessStartInfo startInfo = new ProcessStartInfo(
                Path.Combine(WorkingDirectory.FullName, Command),
                Arguments)
            {
                Verb = "runas", // elevated Admin priveledges
                UseShellExecute = false,
                CreateNoWindow = true,
                WorkingDirectory = WorkingDirectory.FullName,
                RedirectStandardInput = false,
                RedirectStandardOutput = RedirectStandardOutput,
                RedirectStandardError = RedirectStandardError,
            };

            foreach (var env in EnvironmentVariables) 
            {
                if(startInfo.EnvironmentVariables.ContainsKey(env.Key)) {
                    startInfo.EnvironmentVariables.Remove(env.Key);
                }

                startInfo.EnvironmentVariables.Add(env.Key,env.Value);
            }

            this.process.StartInfo = startInfo;
            lock (this.process)
            {
                GSTrace.WriteLine(Command + " " + Arguments);
                if (RedirectStandardError)
                {
                    process.ErrorDataReceived += (sender, e) =>
                    {
                        GSTrace.WriteLine(e.Data);
                    };
                }

                if (RedirectStandardOutput)
                {
                    process.OutputDataReceived += (sender, e) =>
                    {
                        GSTrace.WriteLine(e.Data);
                    };
                }
                process.Start();

                if (RedirectStandardOutput)
                {
                    process.BeginOutputReadLine();
                }

                if (RedirectStandardError)
                {
                    process.BeginErrorReadLine();
                }
            }
        }

        public bool IsRunning()
        {

            bool running = false;
            lock (this.process)
            {
                
                try
                {
                    running = process != null && !process.WaitForExit(0);
                }
                catch (InvalidOperationException)
                {
                    //not started yet
                }
                return running;
            }
        }

        public void Dispose()
        {
            lock (this.process)
            {
                process.Close();
            }
        }


        public void WaitForExit(TimeSpan timeout)
        {
            if (timeout.TotalMilliseconds >= System.Int32.MaxValue )
            {
                process.WaitForExit();
            }
            else
            {
                process.WaitForExit((int)timeout.TotalMilliseconds);
            }
        }
    }
}
