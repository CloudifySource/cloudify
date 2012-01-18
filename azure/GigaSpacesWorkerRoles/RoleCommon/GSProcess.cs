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
        public bool SaveOutput { private get; set; }
        public volatile String output;
        public String Output { get { return this.output;}}
        
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

                if (RedirectStandardOutput || SaveOutput)
                {
                    process.OutputDataReceived += (sender, e) =>
                    {
                        String line = e.Data;
                        if (RedirectStandardOutput)
                        {
                            GSTrace.WriteLine(line);
                        }
                        
                        if (SaveOutput) 
                        {
                            output += line;
                        }
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


        public int WaitForExit(TimeSpan timeout)
        {
            if (timeout.TotalMilliseconds >= System.Int32.MaxValue )
            {
                process.WaitForExit();
                return process.ExitCode;
            }
            else
            {
                if (process.WaitForExit((int)timeout.TotalMilliseconds))
                {
                    return process.ExitCode;
                }
                else
                {
                    // TODO: how can this be done in a clean manner?
                    // currently if interested in return value, wait indefinitely 
                    return 0;
                }
            }
        }

    
    }
}
