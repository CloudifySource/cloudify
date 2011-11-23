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

        public bool StartProxy { get { return StartRestAdmin || StartWebUserInterface; } }
        
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
                    "-Dcom.gigaspaces.system.registryPort=" + PortUtils.XAP_REGISTRY_PORT;
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
                DeployWar("rest", RestAdminWar, PortUtils.XAP_RESTADMIN_PORT, RestAdminMegabytesMemory, RestAdminContextPath);
            }

            if (StartWebUserInterface)
            {
                DeployWar("web-ui",WebuiWar, PortUtils.XAP_WEBUI_PORT, WebuiMegabytesMemory, WebuiContextPath);
            }

            // if this is a managment machine
            if (StartProxy)
            {

                InstallProxyService();

                AddInboudRewrite("rest", "^rest/(.*)", "http://" + IpAddress + ":" + PortUtils.XAP_RESTADMIN_PORT + "/rest/{R:1}");
                //AddOutboundRewrite("rest_outbound", "^/rest/.*", "/rest/{R:1}");

                AddInboudRewrite("webui", "^webui/(.*)", "http://" + IpAddress + ":" + PortUtils.XAP_WEBUI_PORT + "/webui/{R:1}");
                //AddOutboundRewrite("webui_outbound", "^/webui/.*", "/webui/{R:1}"); 

            }
            

            // run until either one of the started processes dies.
            // once that happens this method returns which would eventually cause Azure
            // to trigger a Stop() command which would kill all processes.
            using(gsaProcess) 
            {
                while ( gsaProcess.IsRunning()) 
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

                if (StartProxy)
                {

                    //String localComputerName = Dns.GetHostName();
                    String localComputerName = "127.0.0.1";

                    RemoveInboundRewrite("rest", "^rest/(.*)", "http://" + localComputerName + ":" + PortUtils.XAP_RESTADMIN_PORT + "/rest/{R:1}");
                    //RemoveOutboundRewrite("rest_outbound");                   

                    RemoveInboundRewrite("webui", "^webui/(.*)", "http://" + localComputerName + ":" + PortUtils.XAP_WEBUI_PORT + "/webui/{R:1}");
                    //RemoveOutboundRewrite("webui_outbound");                   

                    UninstallProxyService();
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
            String connectCommand = "connect " + endpoint + ":" + port + RestAdminContextPath + ";";

            return new GSProcess()
            {
                WorkingDirectory = new DirectoryInfo(Path.Combine(XapHomeDirectory.FullName, @"tools\cli")),
                Command = "cloudify.bat",
                Arguments = connectCommand + commands,
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
                GSTrace.WriteLine("Running groovy script with the following environment variables: " + builder.ToString());
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
            long reservedMemoryOnThisMachine =
                GsaMegabytesMemory
                + LusMegabytesMemory
                + GsmMegabytesMemory
                + EsmMegabytesMemory;
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
                    @".name(""" + name + @""")
                    .addContextProperty(""com.gs.application"",""Management"")
                    // All PUs on this role share the same machine. Machines are identified by zone.
                    .sharedMachineProvisioning(""" + SharedMachineIsolationId + @""" ,
                    new org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer()
                    .addGridServiceAgentZone(""" + role + @""")
                    .reservedMemoryCapacityPerMachine(""" + reservedMemoryOnThisMachine + @"m"")
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
                 }"
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
                 admin.getGridServiceManagers().waitFor(" + NumberOfManagementRoleInstances + @");
                 while (admin.getGridServiceManagers().getSize () != " + NumberOfManagementRoleInstances + @") {
                    System.out.println(""Waiting for " + NumberOfManagementRoleInstances + @" Grid Service Managers"");
                    Thread.Sleep(10000);
                 };
                 while (admin.getElasticServiceManagers().getSize() != 1) {
                    System.out.println(""Waiting for one Elastic Service Manager"");
                    Thread.Sleep(10000);
                }
"
                , true);
                managersScript.Run();

                // wait until script completes 
                while (managersScript.IsRunning())
                {
                    Thread.Sleep(10000);
                }
            }
        }

        private class CliExecutionException : Exception {

            public int ErrorCode { private get; set; }

            public CliExecutionException(String message, int errorCode) : base(message + ", errorCode: " + errorCode)
            {
                ErrorCode = errorCode;
            }

        }

        private void InstallProxyService()
        {
         
            String serviceName = "iisproxy";
            String zone = "ui";

            String appRoot = Environment.GetEnvironmentVariable("RoleRoot");

            appRoot = Path.Combine(appRoot + @"\", @"approot\");

            String pathToProxyServiceDirectory = Path.Combine(appRoot, @"iisproxy\").Replace('\\', '/');

            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose Management;")
                .Append("install-service --verbose ")
                .Append("-name ").Append(serviceName).Append(" ")
                .Append("-zone ").Append(zone).Append(" ")
                .Append(pathToProxyServiceDirectory)
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectAll);

            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.FromMinutes(5));
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("Install proxy  failed", exitCode);
            }

        }

        private void UninstallProxyService() 
        {
            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose Management;")
                .Append("uninstall-service --verbose iisproxy")
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectGsa);

            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.FromSeconds(60));
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("Uninstall proxy failed", exitCode);
            }
        }

        private void AddInboudRewrite(String name, String pattern, String rewriteUrl)
        {
            IDictionary<String, String> parameters = new Dictionary<String, String>();
            parameters.Add(new KeyValuePair<String, String>("name", name));
            parameters.Add(new KeyValuePair<String, String>("pattern", pattern));
            parameters.Add(new KeyValuePair<String, String>("rewriteUrl", rewriteUrl));
            parameters.Add(new KeyValuePair<String, String>("patternSyntax", "ECMAScript"));
            parameters.Add(new KeyValuePair<String, String>("stopProcessing", "true"));

            InvokeProxyServiceCommand("rewrite_add", parameters);
        }

        private void AddOutboundRewrite(String name, String conditionPattern, String rewriteUrl)
        {
            IDictionary<String, String> parameters = new Dictionary<String, String>();
            parameters.Add(new KeyValuePair<String, String>("name", name));
            parameters.Add(new KeyValuePair<String, String>("conditionPattern", conditionPattern));
            parameters.Add(new KeyValuePair<String, String>("rewriteUrl", rewriteUrl));

            InvokeProxyServiceCommand("rewrite_outbound_add", parameters);
        }

        private void RemoveInboundRewrite(String name, String pattern, String rewriteUrl)
        {
            IDictionary<String, String> parameters = new Dictionary<String, String>();
            parameters.Add(new KeyValuePair<String, String>("name", name));
            parameters.Add(new KeyValuePair<String, String>("pattern", pattern));
            parameters.Add(new KeyValuePair<String, String>("rewriteUrl", rewriteUrl));
            parameters.Add(new KeyValuePair<String, String>("patternSyntax", "ECMAScript"));
            parameters.Add(new KeyValuePair<String, String>("stopProcessing", "true"));

            InvokeProxyServiceCommand("rewrite_remove", parameters);
        }

        private void RemoveOutboundRewrite(String name)
        {
            IDictionary<String, String> parameters = new Dictionary<String, String>();
            parameters.Add(new KeyValuePair<String, String>("name", name));

            InvokeProxyServiceCommand("rewrite_outbound_remove", parameters);
        }

        private void InvokeProxyServiceCommand(String commandName, IDictionary<String, String> parameters)
        {
            String installServiceCommand = new StringBuilder()
                .Append("use-application --verbose Management;")
                .Append(ProxyServiceCommand(commandName, parameters))
                .ToString();

            GSProcess cliProcess = CliProcess(installServiceCommand, RedirectGsa);

            cliProcess.Run();

            int exitCode = cliProcess.WaitForExit(TimeSpan.FromSeconds(60));
            if (exitCode != 0)
            {
                GSTrace.WriteLine("ExitCode = " + exitCode);
                throw new CliExecutionException("invoke " + commandName + " failed", exitCode);
            }
        }

        private String ProxyServiceCommand(String commandName, IDictionary<String, String> parameters)
        {
            StringBuilder command = new StringBuilder();

            command.Append("invoke --verbose iisproxy ").Append(commandName).Append(" ");

            command.Append("[ ");
            foreach (var param in parameters)
            {
                command.Append("'");
                command.Append(param.Key);
                command.Append("=");
                command.Append(param.Value);
                command.Append("' ");
            }
            command.Append("]");

            return command.ToString();
        }

    }
    
}
