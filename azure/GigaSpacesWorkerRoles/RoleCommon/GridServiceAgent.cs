using System;
using System.Linq;
using System.Diagnostics;
using System.Threading;
using Microsoft.WindowsAzure.ServiceRuntime;
using System.IO;
using System.Collections.Generic;
using System.Text;
using System.Net;

namespace GigaSpaces
{
    public class GridServiceAgent
    {
        volatile bool stopped;

        private readonly GSProcess gsaProcess = new GSProcess();

        private readonly GSProcess webuiProcess = new GSProcess();

        #region Properties
        
        public bool StartGridServiceManager { get { return GsmMegabytesMemory != 0; } }

        public bool StartWebUserInterface { get { return WebuiMegabytesMemory != 0; } }

        public bool StartRestAdmin { get { return RestAdminMegabytesMemory != 0; } }

        public bool StartProxy { get { return StartRestAdmin; } }
        
        public String[] LookupLocators { private get; set; }

        public bool RedirectAll { private get; set; }

        public bool RedirectGsa { private get; set; }

        public bool RedirectWebui { private get; set; }

        public DirectoryInfo XapHomeDirectory { private get; set; }

        public DirectoryInfo JdkHomeDirectory { private get; set; }

        public int GsaMegabytesMemory { private get; set; }

        public int WebuiMegabytesMemory { private get; set; }

        public int GsmMegabytesMemory { private get; set; }

        public int EsmMegabytesMemory { private get; set; }

        public int LusMegabytesMemory { private get; set; }

        public String IpAddress { private get; set; }

        public bool BatchFilesEchoOn { private get; set; }

        public int RestAdminMegabytesMemory { private get; set; }

        public String WebuiContextPath { private get; set; }

        public String RestAdminContextPath { private get; set; }

        public DirectoryInfo TempDirectory { private get; set; }

        private String SharedMachineIsolationId
        {
            get
            {
                return "public";
            }
        }

        private int NumberOfManagementRoleInstances 
        { 
            get 
            {
                return LookupLocators.Length;
            }
        }

        private String JavaOptions
        {
            get
            {
                return
                    (LookupLocators.Length > 0 ? "-Dcom.gs.multicast.enabled=false " : "") +
                    "-Dsun.net.inetaddr.ttl=10 " + // read from hosts file every 10 seconds
                    "-Dcom.gs.transport_protocol.lrmi.bind-port=" + PortUtils.XAP_LRMI_MINPORT + "-" + PortUtils.XAP_LRMI_MAXPORT + " " +
                    "-Dcom.gigaspaces.start.httpPort=" + PortUtils.XAP_WEBSTER_PORT + " " +
                    //"-Dcom.gs.embedded-services.httpd.port="+; +" " +
                    "-Dcom.gigaspaces.system.registryPort=" + PortUtils.XAP_REGISTRY_PORT + " " +
                    "-Djava.io.tmpdir=" + TempDirectory.FullName;
            }
        }

        private FileInfo RestAdminWar
        {
            get
            {
                return new FileInfo(Path.Combine(XapHomeDirectory.FullName,@"tools\rest\rest.war"));

            }
        }

        private FileInfo WebuiWar
        {
            get
            {
                return new FileInfo(Path.Combine(XapHomeDirectory.FullName, @"tools\gs-webui\gs-webui.war"));
            }
        }

        private String ProxyServiceName
        {
            get
            {
                return "iisproxy";
            }
        }

        private String ProxyServiceRole
        {
            get
            {
                //IIS running on management machines
                return "management";
            }
        }

        private String ManagementApplicationName
        {
            get
            {
                return "management";
            }
        }

        private String ManagementSpaceRole
        {
            get
            {
                //management space running on management machines
                return "management";
            }
        }

        private long ReservedMemoryOnCurrentRole
        {
            get
            {
                return GsaMegabytesMemory +
                       (StartGridServiceManager ?
                        LusMegabytesMemory + GsmMegabytesMemory + EsmMegabytesMemory :
                        0);
            }
        }
        
        private IDictionary<String, String> EnvironmentVariables
        {
            get
            {
                var env = new Dictionary<String, String>() 
                {
                    {"VERBOSE", "true"},
                    {"JAVA_HOME", JdkHomeDirectory.FullName},
                    {"JSHOMEDIR", XapHomeDirectory.FullName},
                    {"LOOKUPLOCATORS", String.Join(",", LookupLocators)}
                };
                
                env.Add("GSA_JAVA_OPTIONS",
                    JavaOptions + " " +
                    "-Dcom.gs.zones=" + RoleEnvironment.CurrentRoleInstance.Role.Name +" "+
                    "-Xmx" + GsaMegabytesMemory + "m");

                env.Add("WEBUI_JAVA_OPTIONS",
                    JavaOptions + " " +
                    "-Dcom.gs.webui.port=" + PortUtils.XAP_WEBUI_PORT + " " +
                    "-Xmx" + WebuiMegabytesMemory + "m");

                env.Add("GSM_JAVA_OPTIONS",
                    JavaOptions + " " +
                    "-Xmx" + GsmMegabytesMemory + "m");

                env.Add("LUS_JAVA_OPTIONS",
                    JavaOptions + " " +
                    "-Xmx" + LusMegabytesMemory + "m");

                env.Add("ESM_JAVA_OPTIONS",
                    JavaOptions + " " +
                    "-Xmx" + EsmMegabytesMemory + "m");

                env.Add("NIC_ADDR", IpAddress);
                return env;
            }
        }
        #endregion

        public void Run()
        {
            if (stopped)
            {
                throw new InvalidOperationException("Already stopped.");
            }

            if (String.IsNullOrEmpty(XapHomeDirectory.FullName))
            {
                throw new ArgumentNullException("XapHomeDirectory must not be null nor empty.");
            }

            IpAddress = PortUtils.GetLocalInternalEndpoint();
            
            gsaProcess.WorkingDirectory = new DirectoryInfo(Path.Combine(XapHomeDirectory.FullName, "bin"));
            gsaProcess.Command = "gs-agent.bat";
            if (StartGridServiceManager)
            {
                gsaProcess.Arguments = "gsa.lus 1 gsa.gsm 1 gsa.global.esm 1 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 0";
            }
            else
            {
                gsaProcess.Arguments = "gsa.lus 0 gsa.gsm 0 gsa.global.esm 0 gsa.gsc 0 gsa.global.gsm 0 gsa.global.lus 0";
            }
         
            gsaProcess.EnvironmentVariables = EnvironmentVariables;

            // agent prints its own output to stderr and child processes to stdout
            gsaProcess.RedirectStandardOutput = RedirectAll; 
            gsaProcess.RedirectStandardError = RedirectGsa || RedirectAll;

            gsaProcess.Run();
            WaitForManagers();

            if (StartRestAdmin)
            {
                DeployManagementSpace();
                DeployWar("rest", RestAdminWar, PortUtils.XAP_RESTADMIN_PORT, RestAdminMegabytesMemory, RestAdminContextPath);
                // we can start the proxy only after the rest server is running, since we are using CLI for that
                if (StartProxy)
                {
                    InstallProxyService();
                    AddInboudRewrite("rest", PortUtils.XAP_RESTADMIN_PORT);
                }
            }

            if (StartWebUserInterface)
            {
                DeployWar("web-ui",WebuiWar, PortUtils.XAP_WEBUI_PORT, WebuiMegabytesMemory, WebuiContextPath);
                if (StartProxy)
                {
                    AddInboudRewrite("webui", PortUtils.XAP_WEBUI_PORT);
                }
            }
        }

        /*
         * run until either one of the started processes dies.
           once that happens this method returns which would eventually cause Azure
           to trigger a Stop() command which would kill all processes.
         */
        public void SleepUntilExists()
        {
            using (gsaProcess)
            {
                while (gsaProcess.IsRunning())
                {
                    Thread.Sleep(5000);
                }
            }
        }

        public void Stop(TimeSpan timeout)
        {
            stopped = true;
            try
            {
                if (gsaProcess.IsRunning())
                {
                    ShutdownGridAgentGracefully(timeout);
                }
            }
            finally
            {
                if (gsaProcess.IsRunning() || webuiProcess.IsRunning())
                {
                    //brute force 
                    KillAllJava(timeout);
                }
            }
        }

        /// <summary>
        /// Gracefully shuts down the Grid Service Agent which in turn gracefully shuts down all containers
        /// and all running services on the containers.
        /// </summary>
        /// <param name="timeout"></param>
        private void ShutdownGridAgentGracefully(TimeSpan timeout)
        {
            var shutdownProcess = GroovyProcess(
                        "org.openspaces.admin.machine.Machine m = " +
                            "new org.openspaces.admin.AdminFactory().useDaemonThreads(true).createAdmin().getMachines()" +
                            ".waitFor(\\\"" + IpAddress + "\\\"," + timeout.TotalMilliseconds + ",java.util.concurrent.TimeUnit.MILLISECONDS);" +
                        "if (m != null) { m.getGridServiceAgents().waitForAtLeastOne().shutdown(); };"
                        ,false// too many connection warnings from the script
                        );

            using (shutdownProcess)
            {
                using(gsaProcess)
                {
                    shutdownProcess.Run();
                    gsaProcess.WaitForExit(timeout);
                }
            }
        }

        /// <summary>
        /// Forcefully terminates all java.exe processes running on the machine
        /// </summary>
        /// <param name="timeout"></param>
        private void KillAllJava(TimeSpan timeout)
        {
            var cmd = new FileInfo(Environment.GetEnvironmentVariable("ComSpec"));

            // don't redirect script output
            var killAllJava = new GSProcess()
            {
                WorkingDirectory = cmd.Directory,
                Command = cmd.Name,
                Arguments = "/c taskkill /im java.exe /f",
                RedirectStandardError = true,
                RedirectStandardOutput = true
            };

            using (killAllJava) 
            {
                killAllJava.WaitForExit(timeout);
            }
        }

        /// <summary>
        /// Runs a cli command
        /// </summary>
        GSProcess CliProcess(String commands, Boolean redirectOutput)
        {

            String endpoint = PortUtils.GetInternalEndpoints(PortUtils.XAP_RESTADMIN_PORT)[0];
            int port = PortUtils.XAP_RESTADMIN_PORT;
            String arguments = "\"connect " + endpoint + ":" + port + RestAdminContextPath + ";" + commands+"\"";
            String cloudifyPath = Path.Combine(XapHomeDirectory.FullName, @"tools\cli\cloudify.bat");
            if (redirectOutput) 
            {
                // Build up each line one-by-one and them trim the end
                StringBuilder builder = new StringBuilder();
                foreach (KeyValuePair<String, String> pair in EnvironmentVariables)
                {
                    builder
                        .Append(pair.Key)
                        .Append("=")
                        .Append(pair.Value)
                        .Append(' ');
                }
                
                GSTrace.WriteLine("Running: %COMSPEC% /c call " + cloudifyPath + " " + arguments+"\n Environment Variables="+builder.ToString());
            }
            var cmd = new FileInfo(Environment.GetEnvironmentVariable("ComSpec"));
            return new GSProcess()
            {
                WorkingDirectory = cmd.Directory,
                Command = cmd.Name,
                Arguments = "/c call " + cloudifyPath + " " +arguments,
                RedirectStandardError = redirectOutput,
                RedirectStandardOutput = redirectOutput,
                EnvironmentVariables = EnvironmentVariables
            };
        }

        /// <summary>
        /// Runs a groovy script
        /// </summary>
        /// <param name="groovyCode"></param>
        /// <param name="redirectOutput"></param>
        /// <returns></returns>
        GSProcess GroovyProcess(String groovyCode, Boolean redirectOutput)
        {
            String groovyScript = null;
            do
            {
                groovyScript = Path.Combine(
                    XapHomeDirectory.FullName,
                    Path.ChangeExtension(
                        "GroovyScript_" + Path.GetRandomFileName(),   // groovy class names cannot start with numbers, so we need a prefix
                        ".groovy"));                                // groovy scripts should have the .groovy extension
            } while (File.Exists(groovyScript));

            using (StreamWriter writer = new StreamWriter(groovyScript))
            {
                writer.WriteLine("try {");
                writer.Write(groovyCode);
                writer.WriteLine("} catch (Throwable t) {");
                writer.WriteLine("  org.codehaus.groovy.runtime.StackTraceUtils.sanitize(t);");
                writer.WriteLine("  t.printStackTrace();");
                writer.WriteLine("  System.exit(1);");
                writer.WriteLine("}");
                writer.WriteLine("System.exit(0);");
            }

            String workingDirectory = Path.Combine(XapHomeDirectory.FullName, @"tools\groovy\bin");
            
            // groovy specific options
            var env = new Dictionary<String, String>(EnvironmentVariables) 
            {
                {"JAVA_OPTS","-Xmx512m " + JavaOptions}
            };

            if (redirectOutput)
            {
                // Build up each line one-by-one and them trim the end
                StringBuilder builder = new StringBuilder();
                foreach (KeyValuePair<String, String> pair in env)
                {
                    builder
                        .Append(pair.Key)
                        .Append("=")
                        .Append(pair.Value)
                        .Append(' ');
                }
                GSTrace.WriteLine("Running groovy script \""+groovyCode+"\"\n Environment variables: " + builder.ToString());
            }

            return new GSProcess()
            {
                WorkingDirectory = new DirectoryInfo(workingDirectory),
                Command = "groovy.bat",
                //Arguments = "-e \"" + groovyCode + "\"",
                Arguments = "\"" + groovyScript + "\"",
                RedirectStandardError = redirectOutput,
                RedirectStandardOutput = redirectOutput,
                EnvironmentVariables = env
            };
            
        }
        
        /// <summary>
        /// Uses GigaSpaces XAP admin Groovy API to deploy a WAR file
        /// </summary>
        /// <param name="warFileInfo">Location of WAR file</param>
        /// <param name="megabytesMemory">Java Heap size of the container hosting Jetty running the WAR file</param>
        private void DeployWar(String name, FileInfo warFileInfo , int port, long megabytesMemory, String contextPath)
        {
            var urlPath = contextPath;
            GSTrace.WriteLine("Deploying " + warFileInfo + " (port = " + port + ", context = " + contextPath + ")");
            // need to reserve memory for all non-Gsc (container) processes running on this machine
            String role = RoleEnvironment.CurrentRoleInstance.Role.Name;
            var context = new Dictionary<String, String>() 
            {
                {"web.port", port.ToString()},
                {"web.context", urlPath},
                {"web.context.unique", "true"}
            };

            StringBuilder addContextProperties = new StringBuilder();
            foreach (KeyValuePair<String,String> prop in context) {
                addContextProperties.Append(@".addContextProperty(""" + prop.Key + @""", """ + prop.Value + @""")");
            }
            GSProcess deployScript = GroovyProcess(
               @"org.openspaces.admin.Admin admin = new org.openspaces.admin.AdminFactory().useDaemonThreads(true).createAdmin();
                 admin.getElasticServiceManagers().waitForAtLeastOne();
                 org.openspaces.admin.pu.ProcessingUnit pu = admin.getGridServiceManagers().waitForAtLeastOne().deploy(
                    new org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment(""" + warFileInfo.FullName.Replace("\\", "\\\\") + @""")
                    .memoryCapacityPerContainer(""" + megabytesMemory + @"m"")" +
                    addContextProperties +
//TODO: Add application name prefix only after localcloud adds the prefix too.
//                  @".name("""+ManagementApplicationName+ "." + name + @""")
                    @".name(""" + name + @""")
                    .addContextProperty(""com.gs.application"",""" +ManagementApplicationName+ @""")
                    // All PUs on this role share the same machine. Machines are identified by zone.
                    .sharedMachineProvisioning(""" + SharedMachineIsolationId + @""" ,
                    new org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer()
                    .addGridServiceAgentZone(""" + role + @""")
                    .reservedMemoryCapacityPerMachine(""" + ReservedMemoryOnCurrentRole + @"m"")
                    .create()
                    )
                    // Eager scale (1 container per machine per PU)
                    .scale(
                    new org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer()
                    .atMostOneContainerPerMachine()
                    .create()
                    )
                 );
                 while (!pu.waitFor(1,10000,java.util.concurrent.TimeUnit.MILLISECONDS)) {
                 System.out.println(""waiting for processing unit " + name+ @""");
                 };"
            , true);
            deployScript.Run();
            // wait until script completes (rest admin deployment completes)
            while (deployScript.IsRunning() && gsaProcess.IsRunning())
            {
                Thread.Sleep(10000);
            }

            GSTrace.WriteLine(warFileInfo.Name + " deployed.");
        }

        /// <summary>
        /// Uses GigaSpaces XAP admin Groovy API to wait for grid managers
        /// </summary>
        private void WaitForManagers()
        {
            if (NumberOfManagementRoleInstances > 1)
            {
                GSTrace.WriteLine("Waiting for " + NumberOfManagementRoleInstances + " managers");

                GSProcess managersScript = GroovyProcess(
               @"org.openspaces.admin.Admin admin = new org.openspaces.admin.AdminFactory().useDaemonThreads(true).createAdmin();
                 while (!admin.getGridServiceManagers().waitFor(" + NumberOfManagementRoleInstances + @", 60 , java.util.concurrent.TimeUnit.SECONDS) ||
                        !admin.getElasticServiceManagers().waitFor(1, 60 , java.util.concurrent.TimeUnit.SECONDS)) {
                    System.out.println(""Waiting for " + NumberOfManagementRoleInstances + @" Grid Service Managers and 1 Elastic Service Manager"");
                    /*Due to bug GS-9875*/ 
                    admin.close();
                    admin = new org.openspaces.admin.AdminFactory().useDaemonThreads(true).createAdmin();
                 }"
                , true);
                managersScript.Run();

                // wait until script completes 
                while (managersScript.IsRunning())
                {
                    Thread.Sleep(10000);
                }
            }
        }

        private void DeployManagementSpace()
        {
            GSTrace.WriteLine("Waiting for " + NumberOfManagementRoleInstances + " managers");

            GSProcess managersScript = GroovyProcess(
                @"org.openspaces.admin.Admin admin = new org.openspaces.admin.AdminFactory().useDaemonThreads(true).createAdmin();
                  org.openspaces.admin.pu.ProcessingUnit pu = admin.getGridServiceManagers().waitForAtLeastOne().deploy(
                    new org.openspaces.admin.space.ElasticSpaceDeployment(""cloudifyManagementSpace"")
			        .memoryCapacityPerContainer(""64m"")
			        .highlyAvailable(true)
			        .numberOfPartitions(1)
                    .addContextProperty(""com.gs.application"","""+ManagementApplicationName+@""")
			        // All PUs on this role share the same machine. Machines
			        // are identified by zone.
			        .sharedMachineProvisioning(""" + SharedMachineIsolationId + @""" ,
                        new org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer()
                        .addGridServiceAgentZone(""" + ManagementSpaceRole + @""")
                        .reservedMemoryCapacityPerMachine(""" + ReservedMemoryOnCurrentRole + @"m"")
                        .create()
                        )
			        // Eager scale (1 container per machine per PU)
			        .scale(new org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer()
			                .atMostOneContainerPerMachine()
				            .create()));

                while (pu.waitForSpace(10000,java.util.concurrent.TimeUnit.MILLISECONDS) == null || !pu.getSpace().waitFor(2,10000,java.util.concurrent.TimeUnit.MILLISECONDS)) {
                 System.out.println(""waiting for 2 cloudifyManagementSpace instances"");
                 };"
            , true);
            managersScript.Run();

                // wait until script completes 
                while (managersScript.IsRunning())
                {
                    Thread.Sleep(10000);
                }

            
			

        }

        private class CliExecutionException : Exception {

            public int ErrorCode { get; private set; }

            public CliExecutionException(String message, int errorCode) : base(message + ", errorCode: " + errorCode)
            {
                ErrorCode = errorCode;
            }

        }

        private void InstallProxyService()
        {
            String appRoot = Environment.GetEnvironmentVariable("RoleRoot");

            appRoot = Path.Combine(appRoot + @"\", @"approot\");

            String pathToProxyServiceDirectory = Path.Combine(appRoot, ProxyServiceName+@"\").Replace('\\', '/');

            String pathToProxyServiceProperties = Path.Combine(pathToProxyServiceDirectory, "iisproxy-service.properties");
            using (System.IO.StreamWriter sw = System.IO.File.AppendText(pathToProxyServiceProperties))
            {
                sw.WriteLine();
                String loadBalancerUrl = "http://" + RoleEnvironment.DeploymentId + ".cloudapp.net";
                sw.WriteLine("loadBalancerUrl = \"" + loadBalancerUrl + "\"");
            }

            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose "+ManagementApplicationName+";")
                .Append("install-service --verbose ")
                .Append("-name ").Append(ProxyServiceName).Append(" ")
                .Append("-zone ").Append(ProxyServiceRole).Append(" ")
                .Append(pathToProxyServiceDirectory)
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectAll);
            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.MaxValue);
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("Install proxy  failed", exitCode);
            }

            WaitForRestToDiscoverAllProxyInstances();
        }

        private void WaitForRestToDiscoverAllProxyInstances()
        {
            String listInstancesCommand = new StringBuilder()
                    .Append("use-application --verbose " + ManagementApplicationName + ";")
                    .Append("list-instances --verbose " + ProxyServiceName)
                    .ToString();

            HashSet<Int32> discoveredInstances = new HashSet<Int32>();
            while (discoveredInstances.Count < NumberOfManagementRoleInstances)
            {
                GSProcess cliProcess = CliProcess(listInstancesCommand, RedirectAll);
                cliProcess.SaveOutput = true;
                cliProcess.Run();

                int exitCode = cliProcess.WaitForExit(TimeSpan.MaxValue);
                if (exitCode != 0)
                {
                    GSTrace.WriteLine("ExitCode = " + exitCode);
                    throw new CliExecutionException("Install proxy  failed", exitCode);
                }

                // Parse list-instances output
                // Cloudify instance numbers are 1 based (not zero based)
                for (int i = 1; i <= NumberOfManagementRoleInstances; i++)
                {
                    if (!discoveredInstances.Contains(i))
                    {
                        if (cliProcess.Output.Contains("instance #" + i))
                        {
                            discoveredInstances.Add(i);
                        }
                        else
                        {
                            GSTrace.WriteLine("Waiting for " + ProxyServiceName + " instance #" + i + " to start");
                        }
                    }
                }
                Thread.Sleep(5000);
            }
            GSTrace.WriteLine("Discovered " + NumberOfManagementRoleInstances + " " + ProxyServiceName + " instances");

        }

        private void UninstallProxyService() 
        {
            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose "+ManagementApplicationName+";")
                .Append("uninstall-service --verbose "+ProxyServiceName)
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectGsa);

            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.MaxValue);
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("Uninstall proxy failed", exitCode);
            }
        }

        /*
         * Add an inbound rule to ARR - e.g: route domain.com/applicationName -> some-internal-address/applicationName
         * see iisproxy/issproxy-service.groovy
         */
        private void AddInboudRewrite(String name, long port)
        {
            InvokeProxyServiceCommand("rewrite_add_external_lb", name, Convert.ToString(port));
            GSTrace.WriteLine(name + " reverse proxy inbound rule added.");
        }

        private void RemoveInboundRewrite(String name)
        {
            InvokeProxyServiceCommand("rewrite_remove_external_lb", name);
            GSTrace.WriteLine(name + " reverse proxy inbound rule removed.");
        }

        /* Invoke the 'commandName' custom command of the issproxy service */
        private void InvokeProxyServiceCommand(String commandName, params String[] parameters)
        {
            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose "+ManagementApplicationName + ";")
                .Append(ProxyServiceCommand(commandName, parameters))
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectGsa);

            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.MaxValue);
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("invoke " + commandName + " failed", exitCode);
            }
            
        }

        private String ProxyServiceCommand(String commandName, params String[] parameters)
        {
            return "invoke --verbose "+ ProxyServiceName + " " + commandName + " " + String.Join(" ",parameters);
        }
    }
    
}
